package com.jobshunter.service.application.processors;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobMetadataType;
import com.jobshunter.model.JobPhase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobValidator implements JobProcessor {

  private final Set<String> blacklistDomains;
  private List<Pattern> expiredJobsPatterns;

  public JobValidator(
      ApplicationProperties properties
  ) {
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
    if (context.isAccepted()) {
      context.setPhase(JobPhase.VALIDATED);
      return context;
    }
    if (!context.isRealUrl()) {
      context.setAccepted(false);
      context.setPhase(JobPhase.VALIDATED);
      return context;
    }

    Job job = context.getJob();
    if (this.isValidJobSync(context.getHost(), job.getUrl(), context.getBody())) {
      String desc = context.getDescription() != null ? context.getDescription() : "";
      String serpJobDesc = job.getMetadata(JobMetadataType.SERP_DESCRIPTION);
      if (serpJobDesc != null) {
        desc += "\n" + serpJobDesc;
      }
      desc += "\n" + context.getBody();
      context.setDescription(desc);
      context.setAccepted(true);
    } else {
      context.setAccepted(false);
    }
    context.setPhase(JobPhase.VALIDATED);
    return context;
  }

  private boolean isValidJobSync(String host, String url, String rawBody) {
    log.info("Validating URL: {}", url);
    if (blacklistDomains.contains(host)) {
      log.error("Host is blacklisted {}", url);
      return false;
    }
    log.info("Getting body from URL {}", url);
    try {
      boolean isExpired;
      if (Strings.isBlank(rawBody)) {
        isExpired = true;
        log.error("Invalid URL");
      } else {
        String html = rawBody.toLowerCase();
        isExpired = expiredJobsPatterns.stream().anyMatch(pattern -> pattern.matcher(html).find());
        if (isExpired) {
          log.warn("Invalid URL {}", url);
        } else {
          log.info("URL is valid {}", url);
        }
      }
      return !isExpired;
    } catch (Throwable e) {
      log.error("Invalid URL. Unexpected exception: " + e.getMessage());
      return false;
    }
  }

}
