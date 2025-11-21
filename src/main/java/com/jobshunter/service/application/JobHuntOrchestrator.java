package com.jobshunter.service.application;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.service.clients.ChatGptApiClient;
import com.jobshunter.service.clients.WhatsAppNotifier;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class JobHuntOrchestrator {

  @Autowired
  private ApplicationProperties properties;

  @Autowired
  private ChatGptApiClient chatGptApiClient;

  @Autowired
  private WhatsAppNotifier whatsAppNotifier;

  @Autowired
  private UserJobService userJobService;

  @Autowired
  private com.jobshunter.database.repository.UserRepository userRepository;

  private final AtomicReference<JobHuntResponse> lastRun = new AtomicReference<>();

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

  public void searchJobsForAll(boolean considerLastTime, boolean whatsupNotification) throws InterruptedException {
    LocalDateTime now = LocalDateTime.now();
    for (var user : userRepository.findAll()) {
      if (considerLastTime
          && user.getTimeInterval() != null
          && user.getTimeInterval() > 0
          && user.getLastJobs() != null
          && user.getLastJobs().plusMinutes(user.getTimeInterval()).isAfter(now)) {
        continue;
      }

      JobHuntResponse response = this.runInternal(user.getPrompt(), user.getUsername(), user.getCvFileId());
      user.setLastJobs(LocalDateTime.now());
      userRepository.save(user);
      userJobService.saveJobsForUser(user.getUsername(), response.jobsFound());
      if (whatsupNotification) {
        this.notifyWhatsupp(user.getUsername(), response);
      }
      Thread.sleep(properties.getIterationDelay());
    }
  }

  public JobHuntResponse lastRun() {
    return lastRun.get();
  }

  private JobHuntResponse runInternal(String prompt, String username, String chatGptFileId) {
    Set<String> jobs = new HashSet<>();
    for (int i = 0; i < properties.getIterationPerUser(); i++) {
      List<String> existingUrls = userJobService.getExistingJobUrlsForUser(username);
      String promptForChatGpt = prompt;
      if (!existingUrls.isEmpty()) {
        promptForChatGpt = promptForChatGpt + ".  Exclude those url's: " + String.join(", ", existingUrls) + ".";
      }
      log.info("Searching jobs for user {} iteration {}", username, i);
      jobs.addAll(chatGptApiClient.search(promptForChatGpt, chatGptFileId));
      try {
        Thread.sleep(properties.getIterationDelay());
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    JobHuntResponse summary = new JobHuntResponse(jobs.stream().filter(this::isValidJob).toList());
    lastRun.set(summary);
    return summary;
  }

  private void notifyWhatsupp(String username, JobHuntResponse summary) {
    if (!summary.jobsFound().isEmpty()) {
      whatsAppNotifier.send(summary.jobsFound(), username);
    }
  }

  private boolean isValidJob(String jobURL) {
    log.info("Testing URL {} ", jobURL);
    HttpHeaders headers = new HttpHeaders();
    headers.set("User-Agent", "Mozilla/5.0");
    headers.setAccept(List.of(MediaType.TEXT_HTML));
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    try {
      ResponseEntity<String> response = restTemplate.exchange(
          URI.create(jobURL),
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

}
