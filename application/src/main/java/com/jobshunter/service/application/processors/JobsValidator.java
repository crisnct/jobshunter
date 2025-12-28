package com.jobshunter.service.application.processors;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobPhase;
import com.jobshunter.service.clients.BrowserSimulator;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobsValidator implements JobProcessor {

  private final BrowserSimulator browserSimulator;

  private List<Pattern> expiredJobsPatterns;

  private final Set<String> blacklistDomains;

  public JobsValidator(
      ApplicationProperties properties,
      BrowserSimulator browserSimulator
  ) {
    this.browserSimulator = browserSimulator;
    this.blacklistDomains = Arrays.stream(properties.getJobsHunter().getBlacklist().split(","))
        .map(String::trim)
        .map(String::toLowerCase)
        .collect(Collectors.toUnmodifiableSet());

    expiredJobsPatterns = new ArrayList<>();
    for (String keyword : properties.getJobsHunter().getExpiredExpressions().split(",")) {
      expiredJobsPatterns.add(Pattern.compile(">[^<]{0,500}" + Pattern.quote(keyword) + "[^<]{0,500}<",
          Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
    }
    expiredJobsPatterns = Collections.unmodifiableList(expiredJobsPatterns);
  }

  @Override
  public JobContext processAsync(JobContext context) {
    Job job = context.getJob();
    Pair<Boolean, String> validJob = isValidJob(job.getUrl());
    if (validJob.getFirst()) {
      String desc = job.getDescription() != null ? job.getDescription() : "";
      desc += "\n" + cleanupHTML(validJob.getSecond());
      job.setDescription(desc);
      context.setAccepted(true);
    } else {
      context.setAccepted(false);
    }
    context.setPhase(JobPhase.VALIDATED);
    return context;
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
      ResponseEntity<String> response = browserSimulator.openPage(uri.toString()).toCompletableFuture().get();
      String body = response.getBody();
      String html = body.toLowerCase();
      boolean isExpired = expiredJobsPatterns.stream().anyMatch(pattern -> pattern.matcher(html).find());
      if (isExpired) {
        log.warn("Invalid URL: {}", jobURL);
      } else {
        log.info("URL is valid: {}", jobURL);
      }
      return Pair.of(!isExpired, isExpired ? "" : body);
    } catch (Throwable e) {
      log.error("Invalid URL: {}-{}", jobURL,e.getMessage());
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
