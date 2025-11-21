package com.jobshunter.service.application;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.database.entities.User;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.service.clients.ChatGptApiClient;
import com.jobshunter.service.clients.WhatsAppNotifier;
import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class JobHuntService {

  @Autowired
  private ApplicationProperties properties;

  @Autowired
  private ChatGptApiClient chatGptApiClient;

  @Autowired
  private WhatsAppNotifier whatsAppNotifier;

  @Autowired
  private UserDataService userDataService;

  private List<Pattern> expiredKeywordsPatterns;

  @Autowired
  private RestTemplate restTemplate;

  @PostConstruct
  public void init() {
    expiredKeywordsPatterns = new ArrayList<>();
    for (String keyword : properties.getExpiredKeywords().split(",")) {
      expiredKeywordsPatterns.add(Pattern.compile(">[A-Za-z0-9 .,!?\\-()]*" + Pattern.quote(keyword) + "[A-Za-z0-9 .,!?\\-()]*<",
          Pattern.CASE_INSENSITIVE));
    }
    expiredKeywordsPatterns = Collections.unmodifiableList(expiredKeywordsPatterns);
  }

  @Scheduled(fixedRateString = "${jobshunter.scheduler.frequency:3600000}")
  public void scheduledRun() throws InterruptedException {
    log.info("Starts scheduled job hunt...");
    for (var user : userDataService.getAllUsers()) {
      this.searchJobsForUser(true, user)
          .ifPresent(jobs -> this.notifyWhatsupp(user.getUsername(), jobs));
      Thread.sleep(properties.getIterationDelay());
    }
    log.info("Stop scheduled job hunt.");
  }

  public Optional<JobHuntResponse> searchJobsForUser(boolean considerLastTime, User user) {
    if (considerLastTime
        && user.getTimeInterval() != null
        && user.getTimeInterval() > 0
        && user.getLastJobs() != null
        && user.getLastJobs().plusMinutes(user.getTimeInterval()).isAfter(LocalDateTime.now())) {
      return Optional.empty();
    }

    JobHuntResponse response = this.searchJobsForUser(user.getPrompt(), user.getUsername(), user.getCvFileId());
    user.setLastJobs(LocalDateTime.now());
    userDataService.updateUser(user);

    response.jobsFound().stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .forEach(url -> userDataService.addJobUrl(user, url));

    return Optional.of(response);
  }

  private JobHuntResponse searchJobsForUser(String prompt, String username, String chatGptFileId) {
    Set<String> jobs = new HashSet<>();
    for (int i = 0; i < properties.getIterationPerUser(); i++) {
      List<String> existingUrls = userDataService.getExistingJobUrlsForUser(username);
      String promptForChatGpt = prompt;
      if (!existingUrls.isEmpty()) {
        promptForChatGpt = promptForChatGpt + ".  Do not send me those url's: " + String.join(", ", existingUrls) + ".";
      }
      log.info("Searching jobs for user {} iteration {}", username, i);
      jobs.addAll(chatGptApiClient.search(promptForChatGpt, chatGptFileId));
      try {
        //noinspection BusyWait
        Thread.sleep(properties.getIterationDelay());
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    return new JobHuntResponse(jobs.stream().filter(this::isValidJob).toList());
  }

  public void notifyWhatsupp(String username, JobHuntResponse summary) {
    if (!summary.jobsFound().isEmpty()) {
      whatsAppNotifier.send(summary.jobsFound(), username);
    }
  }

  private boolean isValidJob(String jobURL) {
    URI uri = toSafeHttpUri(jobURL);
    if (uri == null) {
      log.warn("Skipping URL {} because it is not a permitted HTTP/HTTPS target", jobURL);
      return false;
    }
    log.info("Testing URL {} ", uri);
    HttpHeaders headers = new HttpHeaders();
    headers.set("User-Agent", "Mozilla/5.0");
    headers.setAccept(List.of(MediaType.TEXT_HTML));
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    try {
      ResponseEntity<String> response = restTemplate.exchange(
          uri,
          HttpMethod.GET,
          entity,
          String.class
      );

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        String html = response.getBody();
        boolean isExpired = expiredKeywordsPatterns.stream().anyMatch(pattern -> pattern.matcher(html).find());
        System.out.println("Expired: " + isExpired + " " + jobURL);
        return !isExpired;
      } else {
        return false;
      }
    } catch (Exception e) {
      return false;
    }
  }

  private URI toSafeHttpUri(String jobURL) {
    if (jobURL == null || jobURL.isBlank()) {
      return null;
    }
    try {
      URI uri = URI.create(jobURL.trim());
      String scheme = uri.getScheme();
      if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
        return null;
      }
      String host = uri.getHost();
      if (host == null || host.isBlank()) {
        return null;
      }
      InetAddress address = InetAddress.getByName(host);
      if (address.isAnyLocalAddress()
          || address.isLoopbackAddress()
          || address.isLinkLocalAddress()
          || address.isSiteLocalAddress()
          || address.isMulticastAddress()) {
        return null;
      }
      return uri;
    } catch (Exception ex) {
      return null;
    }
  }

}
