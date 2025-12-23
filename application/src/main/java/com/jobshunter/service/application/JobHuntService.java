package com.jobshunter.service.application;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.EngineType;
import com.jobshunter.dto.Job;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.dto.SearchJobOrder;
import com.jobshunter.service.application.hunting.GeminiJobHunting;
import com.jobshunter.service.application.hunting.GptJobHunting;
import com.jobshunter.service.application.hunting.JobHunting;
import com.jobshunter.service.application.hunting.SerpJobHunting;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import com.jobshunter.service.application.notifiers.WhatsappNotifierService;
import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
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
public class JobHuntService {

  @Autowired
  private ApplicationProperties properties;

  @Autowired
  private WhatsappNotifierService whatsappNotifierService;

  @Autowired
  private EmailNotifierService emailNotifierService;

  @Autowired
  private UserDataService userDataService;

  @Autowired
  private SerpJobHunting serpJobHunting;

  @Autowired
  private GptJobHunting gptJobHunting;

  @Autowired
  private GeminiJobHunting geminiJobHunting;

  private List<Pattern> expiredJobsPatterns;

  @Autowired
  private RestTemplate restTemplate;

  @PostConstruct
  public void init() {
    expiredJobsPatterns = new ArrayList<>();
    for (String keyword : properties.getJobsHunter().getExpiredExpressions().split(",")) {
      expiredJobsPatterns.add(Pattern.compile(">[^<]{0,500}" + Pattern.quote(keyword) + "[^<]{0,500}<",
          Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
    }
    expiredJobsPatterns = Collections.unmodifiableList(expiredJobsPatterns);
  }

  public void scheduledRun() {
    log.info("Starts scheduled job hunt...");
    for (var user : userDataService.getAllUsers()) {
      if (user.isNotifyWhatsapp() || user.isNotifyEmail()) {
        if (user.getTimeInterval() != null
            && user.getTimeInterval() > 0
            && user.getLastJobs() != null
            && user.getLastJobs().plusMinutes(user.getTimeInterval()).isBefore(LocalDateTime.now())) {
          log.info("Start searching jobs for {} ", user.getUsername());
          this.searchJobsForUser(new SearchJobOrder(
              user,
              List.of(EngineType.GPT4),
              properties.getJobsHunter().getIterationPerUser()
          ));
        }
      }
    }
    log.info("Stop scheduled job hunt.");
  }

  public JobHuntResponse searchJobsForUser(SearchJobOrder order) {
    UserEntity user = order.user();
    final JobsSynchronizer jobsSync =
        new JobsSynchronizer(userDataService.getExistingJobUrlsForUser(user.getUsername()), this::isValidJob);

    List<CompletableFuture<Void>> enginesFutures = new ArrayList<>();
    this.searchJobs("SERP", serpJobHunting, jobsSync, order, enginesFutures);
    this.searchJobs("GPT", gptJobHunting, jobsSync, order, enginesFutures);
    this.searchJobs("GEMINI", geminiJobHunting, jobsSync, order, enginesFutures);

    CompletableFuture.allOf(enginesFutures.toArray(CompletableFuture[]::new)).join();

    JobHuntResponse jobHuntResponse = new JobHuntResponse(jobsSync.getJobs().stream()
        .sorted(Comparator.comparing(Job::score).reversed())
        .toList());

    List<Job> jobs = jobHuntResponse.jobsFound();
    if (jobs.isEmpty()) {
      log.info("No jobs found for user {} ", user.getUsername());
    } else if (user.isNotifyEmail() || user.isNotifyWhatsapp()) {
      userDataService.updateUser(user, jobs);
      log.info("Found {} jobs for {} ", jobs.size(), user.getEmail());
      jobs.forEach(System.out::println);

      if (user.isNotifyWhatsapp()) {
        whatsappNotifierService.send(jobs, user);
      }
      if (user.isNotifyEmail()) {
        emailNotifierService.sendUsingTemplate(jobs, user);
      }
    }

    return jobHuntResponse;
  }

  private void searchJobs(String enginePrefix, JobHunting jobHunting, JobsSynchronizer jobsSync, SearchJobOrder order,
      List<CompletableFuture<Void>> enginesFutures) {
    List<EngineType> enginesFiltered = order.engines().stream().filter(p -> p.name().startsWith(enginePrefix)).toList();
    if (!enginesFiltered.isEmpty()) {
      SearchJobOrder orderClone = new SearchJobOrder(
          order.user(),
          enginesFiltered,
          order.iterations()
      );
      enginesFutures.add(jobHunting.searchJobs(jobsSync, orderClone));
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
        String html = Jsoup.parse(response.getBody()).text().toLowerCase();
        boolean isExpired = expiredJobsPatterns.stream().anyMatch(pattern -> pattern.matcher(html).find());
        log.info("Expired: {} {}", isExpired, jobURL);
        return !isExpired;
      } else {
        log.info("Expired: true {}", jobURL);
        return false;
      }
    } catch (Exception e) {
      log.info("Expired: true {} {}", jobURL, e.getMessage());
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
