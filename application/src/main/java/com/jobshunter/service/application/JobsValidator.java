package com.jobshunter.service.application;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.model.Job;
import com.jobshunter.service.clients.BrowserSimulator;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobsValidator {

  private final Executor jobsValidatorExecutor;

  private final ApplicationProperties properties;

  private final BrowserSimulator browserSimulator;

  private final Set<String> blacklistDomains;

  public JobsValidator(
      @Qualifier("jobsValidatorExecutor") Executor jobsValidatorExecutor,
      ApplicationProperties properties,
      BrowserSimulator browserSimulator
  ) {
    this.jobsValidatorExecutor = jobsValidatorExecutor;
    this.properties = properties;
    this.browserSimulator = browserSimulator;
    this.blacklistDomains = Arrays.stream(properties.getJobsHunter().getBlacklist().split(","))
        .map(String::trim)
        .map(String::toLowerCase)
        .collect(Collectors.toUnmodifiableSet());
  }

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
    Pair<Boolean, String> validJob = isValidJob(job.getUrl());
    if (validJob.getFirst()) {
      String desc = job.getDescription() != null ? job.getDescription() : "";
      desc += "\n" + cleanupHTML(validJob.getSecond());
      job.setDescription(desc);
    } else {
      invalidURLs.add(job.getUrl());
    }
  }

  private String cleanupHTML(String body) {
    Document document = Jsoup.parse(body);
    document.select("script, style, nav, footer, header, aside").remove();
    document.select("button, a").remove();
    return document.text();
  }

  private Pair<Boolean, String> isValidJob(String jobURL) {
    log.info("Validating URL: {}", jobURL);
    URI uri = toSafeHttpUri(jobURL);
    if (uri == null) {
      log.error("Skipping URL {} because it is not a permitted HTTP/HTTPS target", jobURL);
      return Pair.of(false, "");
    }
    String domain = uri.getHost();
    if (domain.startsWith("www.")) {
      domain = domain.substring(4);
    }
    if (blacklistDomains.contains(domain)) {
      log.error("URL is blacklisted {}", jobURL);
      return Pair.of(false, "");
    }
    log.info("Getting body from URL {}", jobURL);
    try {
      String body = browserSimulator.openPage(uri.toString()).getBody();
      String html = body.toLowerCase();
      boolean isExpired = expiredJobsPatterns.stream().anyMatch(pattern -> pattern.matcher(html).find());
      if (isExpired) {
        log.warn("Invalid URL: {}", jobURL);
      } else {
        log.info("URL is valid: {}", jobURL);
      }
      return Pair.of(!isExpired, isExpired ? "" : body);
    } catch (Throwable e) {
      log.error("Invalid URL: {}", jobURL);
      return Pair.of(false, "");
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
