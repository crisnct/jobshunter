package com.jobshunter.service.application.processors;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobMetadataType;
import com.jobshunter.model.JobPhase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class JobValidatorProcessor implements JobProcessor {

  private List<Pattern> expiredJobsPatterns;

  public JobValidatorProcessor(
      ApplicationProperties properties
  ) {
    expiredJobsPatterns = new ArrayList<>();
    for (String keyword : properties.getJobsHunter().getExpiredExpressions().split(",")) {
      expiredJobsPatterns.add(
          Pattern.compile("(?i)[^a-zA-Z0-9]{0,20}" + Pattern.quote(keyword.toLowerCase()) + "[^a-zA-Z0-9]{0,20}",
              Pattern.DOTALL
          )
      );
    }
    expiredJobsPatterns = Collections.unmodifiableList(expiredJobsPatterns);
  }

  @Override
  public JobContext processAsync(JobContext context) {
    Job job = context.getJob();
    if (this.isValidJobSync(job.getUrl(), context.getBody())) {
      String desc = context.getDescription() != null ? context.getDescription() : "";
      String serpJobDesc = job.getMetadata(JobMetadataType.SERP_DESCRIPTION);
      if (serpJobDesc != null) {
        desc += "\n" + serpJobDesc;
      }
      desc += "\n" + context.getBody();
      context.setDescription(desc);
      context.setValidatedSuccessfully(true);
      context.setPhase(JobPhase.VALIDATION);
    } else {
      context.setValidatedSuccessfully(false);
      context.failJob("Job is not validated successfully");
    }

    return context;
  }

  private boolean isValidJobSync(String url, String rawBody) {
    log.info("Validating URL, getting body of the page: {}", url);
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
