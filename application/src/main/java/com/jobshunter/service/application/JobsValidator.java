package com.jobshunter.service.application;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.dto.Job;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class JobsValidator {

  public static final ThreadLocal<HttpClientContext> HTTP_CONTEXT =
      new ThreadLocal<>();

  @Autowired
  @Qualifier("jobsValidatorExecutor")
  private Executor jobsValidatorExecutor;

  @Autowired
  private ApplicationProperties properties;

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private RestClient restClient;

  private List<Pattern> expiredJobsPatterns;

  @PostConstruct
  public void init() {
    expiredJobsPatterns = new ArrayList<>();
    for (String keyword : properties.getJobsHunter().getExpiredExpressions().split(",")) {
      expiredJobsPatterns.add(Pattern.compile(">[^<]{0,500}" + Pattern.quote(keyword) + "[^<]{0,500}<",
          Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
    }
    expiredJobsPatterns = Collections.unmodifiableList(expiredJobsPatterns);
  }

  public List<Job> validateJobs(List<Job> jobs) {
    Set<String> invalidURL = ConcurrentHashMap.newKeySet(50);
    List<CompletableFuture<Void>> redirectionFutures = new ArrayList<>();
    for (Job job : jobs) {
      redirectionFutures.add(CompletableFuture.runAsync(() -> updateURL(job), jobsValidatorExecutor));
    }
    for (Job job : jobs) {
      redirectionFutures.add(CompletableFuture.runAsync(() -> validateURL(job, invalidURL), jobsValidatorExecutor));
    }
    CompletableFuture.allOf(redirectionFutures.toArray(CompletableFuture[]::new)).join();
    return jobs.stream().filter(job -> !invalidURL.contains(job.getUrl())).toList();
  }

  private void updateURL(@NotNull Job job) {
    try {
      restClient.get()
          .uri(job.getUrl())
          .retrieve()
          .toBodilessEntity();
      HttpClientContext context = HTTP_CONTEXT.get();
      URI finalUri =
          context.getRedirectLocations() == null || context.getRedirectLocations().size() == 0
              ? context.getRequest().getUri()
              : context.getRedirectLocations().get(context.getRedirectLocations().size() - 1);
      job.setUrl(finalUri.toString());
    } catch (Exception e) {
      e.printStackTrace();
      //
    }
  }

  private void validateURL(@NotNull Job job, Set<String> invalidURLs) {
    if (!isValidJob(job.getUrl())) {
      invalidURLs.add(job.getUrl());
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
