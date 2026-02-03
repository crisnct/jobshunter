package com.jobshunter.service.application.processors;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.ApplicationProperties.JobsHunter;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobMetadataType;
import com.jobshunter.model.JobPhase;
import com.jobshunter.service.application.processors.validation.PatternCache;
import com.jobshunter.service.application.processors.validation.ValidationContext;
import com.jobshunter.service.application.processors.validation.ValidationRule;
import com.jobshunter.service.application.processors.validation.rules.B2BEorLocalRule;
import com.jobshunter.service.application.processors.validation.rules.B2BRemoteRule;
import com.jobshunter.service.application.processors.validation.rules.EmploymentLocalRule;
import com.jobshunter.service.application.processors.validation.rules.NotExpiredRule;
import com.jobshunter.service.application.processors.validation.rules.OnsiteHybridRule;
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

  private final PatternCache patternCache;
  private final List<Pattern> freelancerPattern;
  private final List<Pattern> remotePattern;
  private final NotExpiredRule notExpiredRule;
  private final List<ValidationRule> jobTypeRules;

  public JobValidatorProcessor(ApplicationProperties properties, PatternCache patternCache) {
    this.patternCache = patternCache;
    JobsHunter jobsHunter = properties.getJobsHunter();
    List<Pattern> expiredJobsPatterns = this.parseExpressions(jobsHunter.getExpiredExpressions());
    this.freelancerPattern = this.parseExpressions(jobsHunter.getFreelancerExpressions());
    this.remotePattern = this.parseExpressions(jobsHunter.getRemoteExpressions());

    this.notExpiredRule = new NotExpiredRule(expiredJobsPatterns);
    this.jobTypeRules = List.of(
        new OnsiteHybridRule(),
        new B2BEorLocalRule(),
        new B2BRemoteRule(),
        new EmploymentLocalRule()
    );
  }

  private List<Pattern> parseExpressions(String expressions) {
    List<Pattern> result = new ArrayList<>();
    for (String keyword : expressions.split(",")) {
      result.add(patternCache.getWordPattern(keyword));
    }
    return Collections.unmodifiableList(result);
  }

  @Override
  public JobContext processAsync(JobContext context) {
    if (this.isValidJobSync(context)) {
      String desc = context.getDescription() != null ? context.getDescription() : "";
      String serpJobDesc = context.getJob().getMetadata(JobMetadataType.SERP_DESCRIPTION);
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

  private boolean isValidJobSync(JobContext jobContext) {
    String url = jobContext.getJob().getUrl();
    log.info("Validating URL, getting body of the page: {}", url);
    try {
      if (Strings.isBlank(jobContext.getBody())) {
        log.error("Invalid URL - empty body");
        return false;
      }

      ValidationContext ctx = buildValidationContext(jobContext);

      // Check if job is expired first
      if (!notExpiredRule.validate(ctx).isValid()) {
        log.warn("Job is expired: {}", url);
        return false;
      }

      // Check job type rules - any match means valid
      boolean isValid = jobTypeRules.stream()
          .anyMatch(rule -> rule.validate(ctx).isValid());

      if (isValid) {
        log.info("URL is valid {}", url);
      } else {
        log.info("URL is valid but does not match user preferences {}", url);
      }
      return isValid;
    } catch (Throwable e) {
      log.error("Invalid URL. Unexpected exception: " + e.getMessage());
      return false;
    }
  }

  private ValidationContext buildValidationContext(JobContext jobContext) {
    String html = jobContext.getBody().toLowerCase();
    boolean cityMatch = patternCache.matchesWord(jobContext.getUser().getCity(), html);
    boolean countryMatch = patternCache.matchesWord(jobContext.getUser().getCountry(), html);
    boolean freelancerRole = freelancerPattern.stream().anyMatch(p -> p.matcher(html).find());
    boolean remoteRole = remotePattern.stream().anyMatch(p -> p.matcher(html).find());

    return ValidationContext.builder()
        .html(html)
        .jobContext(jobContext)
        .cityMatch(cityMatch)
        .countryMatch(countryMatch)
        .freelancerRole(freelancerRole)
        .remoteRole(remoteRole)
        .build();
  }

}
