package com.jobshunter.service.application;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.dto.Job;
import com.jobshunter.service.clients.BrowserSimulator;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobsValidator {

  @Autowired
  @Qualifier("jobsValidatorExecutor")
  private Executor jobsValidatorExecutor;

  @Autowired
  private ApplicationProperties properties;

  @Autowired
  private BrowserSimulator browserSimulator;

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
    log.info("JobsValidator will validate {} url's", jobs.size());
    Set<String> invalidURL = ConcurrentHashMap.newKeySet(50);

    //In case of redirects get the last redirect URL
    List<CompletableFuture<Void>> redirectionFutures = new ArrayList<>();
    for (Job job : jobs) {
      redirectionFutures.add(CompletableFuture.runAsync(() -> updateURL(job), jobsValidatorExecutor));
    }
    CompletableFuture.allOf(redirectionFutures.toArray(CompletableFuture[]::new)).join();

    //Validate URL's
    redirectionFutures = new ArrayList<>();
    for (Job job : jobs) {
      redirectionFutures.add(CompletableFuture.runAsync(() -> validateURL(job, invalidURL), jobsValidatorExecutor));
    }
    CompletableFuture.allOf(redirectionFutures.toArray(CompletableFuture[]::new)).join();

    List<Job> result = jobs.stream().filter(job -> !invalidURL.contains(job.getUrl())).toList();
    log.info("JobsValidator detected that only {} url's are valid", result.size());
    return result;
  }

  private void updateURL(@NotNull Job job) {
    String newURL = browserSimulator.getFinalRedirectedURL(job.getUrl());
    job.setUrl(newURL);
  }

  private void validateURL(@NotNull Job job, Set<String> invalidURLs) {
    if (!isValidJob(job.getUrl())) {
      invalidURLs.add(job.getUrl());
    }
  }

  private boolean isValidJob(String jobURL) {
    URI uri = toSafeHttpUri(jobURL);
    if (uri == null) {
      log.error("Skipping URL {} because it is not a permitted HTTP/HTTPS target", jobURL);
      return false;
    }
    try {
      String body = browserSimulator.openPage(uri.toString()).getBody();
      String html = body.toLowerCase();
      boolean isExpired = expiredJobsPatterns.stream().anyMatch(pattern -> pattern.matcher(html).find());
      if (isExpired) {
        log.warn("Invalid URL: {}", jobURL);
      } else {
        log.info("Valid URL: {}", jobURL);
      }
      return !isExpired;
    } catch (Throwable e) {
      log.error("Invalid URL: {}", jobURL);
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
      log.error("Error at validating url:\n{}", jobURL, ex);
      return null;
    }
  }

}
