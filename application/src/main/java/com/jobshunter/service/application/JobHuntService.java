package com.jobshunter.service.application;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.Job;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.dto.SearchWithSerpRequest;
import com.jobshunter.dto.SerpApiJobHit;
import com.jobshunter.dto.SerpApiJobsResult;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import com.jobshunter.service.application.notifiers.WhatsappNotifierService;
import com.jobshunter.service.clients.gpt.GptApi4Client;
import com.jobshunter.service.clients.gpt.GptApi5Client;
import com.jobshunter.service.clients.serpapi.SerpApiClient;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
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
  private GptApi5Client gpt5Client;

  @Autowired
  private GptApi4Client gpt4Client;

  @Autowired
  private SerpApiClient serpApiClient;

  @Autowired
  private WhatsappNotifierService whatsappNotifierService;

  @Autowired
  private EmailNotifierService emailNotifierService;

  @Autowired
  private UserDataService userDataService;

  private List<Pattern> expiredJobsPatterns;

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private JsonMapper mapper;

  @PostConstruct
  public void init() {
    expiredJobsPatterns = new ArrayList<>();
    for (String keyword : properties.getJobsHunter().getExpiredExpressions().split(",")) {
      expiredJobsPatterns.add(Pattern.compile(">[^<]{0,500}" + Pattern.quote(keyword) + "[^<]{0,500}<",
          Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
    }
    expiredJobsPatterns = Collections.unmodifiableList(expiredJobsPatterns);
  }

  public void scheduledRun() throws InterruptedException {
    log.info("Starts scheduled job hunt...");
    for (var user : userDataService.getAllUsers()) {
      if (user.isNotifyWhatsapp() || user.isNotifyEmail()) {
        if (user.getTimeInterval() != null
            && user.getTimeInterval() > 0
            && user.getLastJobs() != null
            && user.getLastJobs().plusMinutes(user.getTimeInterval()).isBefore(LocalDateTime.now())) {
          log.info("Start searching jobs for {} ", user.getUsername());
          JobHuntResponse jobs = this.searchJobsForUser(user, properties.getJobsHunter().getIterationPerUser());
          log.info("Found {} jobs for {} ", jobs.jobsFound().size(), user.getEmail());
          jobs.jobsFound().forEach(System.out::println);
          if (!jobs.jobsFound().isEmpty()) {
            userDataService.updateUser(user, jobs.jobsFound());
            if (user.isNotifyWhatsapp()) {
              this.notifyWhatsApp(user, jobs);
            }
            if (user.isNotifyEmail()) {
              this.notifyEmail(user, jobs);
            }
          }
          Thread.sleep(properties.getJobsHunter().getIterationDelay());
        }
      }
    }
    log.info("Stop scheduled job hunt.");
  }

  public JobHuntResponse searchJobsForUser(UserEntity user, int iterations) {
    if (Strings.isEmpty(user.getPrompt()) || Strings.isEmpty(user.getCvFileId())) {
      log.info("Skip user {} because prompt or cv is missing", user.getUsername());
      return new JobHuntResponse(Collections.emptyList());
    }

    Map<String, Job> jobs = new HashMap<>();
    List<String> existingUrls = new ArrayList<>(userDataService.getExistingJobUrlsForUser(user.getUsername()));

    if (user.getSerpApiRequest() != null) {
      List<Job> jobsFound = this.searchWithSerpAPi(user);
      jobsFound.forEach(newJob -> {
        if (!existingUrls.contains(newJob.url()) && isValidJob(newJob.url())) {
          jobs.put(newJob.url(), newJob);
        }
        existingUrls.add(newJob.url());
      });
    }

    for (int i = 0; i < iterations; i++) {
      log.info("Searching jobs for user {} iteration {}", user.getUsername(), i);
      List<Job> jobsFound = this.gpt4Client.search(user.getPrompt(), user.getCvFileId());
      jobsFound.forEach(newJob -> {
        if (!existingUrls.contains(newJob.url()) && isValidJob(newJob.url())) {
          jobs.put(newJob.url(), newJob);
        }
        existingUrls.add(newJob.url());
      });

      if (iterations > 1) {
        try {
          Thread.sleep(properties.getJobsHunter().getIterationDelay());
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }

    return new JobHuntResponse(jobs.values().stream()
        .sorted(Comparator.comparing(Job::score).reversed())
        .toList());
  }

  private List<Job> searchWithSerpAPi(UserEntity user){
    List<Job> jobsFound = new ArrayList<>();
    try {
      SearchWithSerpRequest request = mapper.readValue(user.getSerpApiRequest(), SearchWithSerpRequest.class);
      SerpApiJobsResult serpApiResult = serpApiClient.searchJobs(request);

      for (SerpApiJobHit job: serpApiResult.jobs()){
        String jobDescription = job.description() + "\n" + job.highlights();
        int score = gpt4Client.computeScore(jobDescription, user.getCvFileId());
        jobsFound.add(new Job(score, job.applyLinks().getFirst(), "Google"));
      }
    } catch (IOException e) {
      log.error("Error at parsing response", e);
    }
    return jobsFound;
  }

  public void notifyWhatsApp(UserEntity user, JobHuntResponse summary) {
    whatsappNotifierService.send(summary.jobsFound(), user);
  }

  public void notifyEmail(UserEntity user, JobHuntResponse summary) {
    emailNotifierService.sendUsingTemplate(summary.jobsFound(), user);
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
