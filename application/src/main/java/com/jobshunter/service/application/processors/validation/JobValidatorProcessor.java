package com.jobshunter.service.application.processors.validation;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.config.ApplicationProperties.JobsHunter;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobPhase;
import com.jobshunter.service.application.processors.JobProcessor;
import com.jobshunter.service.application.processors.validation.rules.B2BJobsRule;
import com.jobshunter.service.application.processors.validation.rules.EORJobsRule;
import com.jobshunter.service.application.processors.validation.rules.LocalJobsRule;
import com.jobshunter.service.application.processors.validation.rules.NotExpiredRule;
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
  private final List<Pattern> localJobPattern;
  private final List<Pattern> remotePattern;
  private final NotExpiredRule notExpiredRule;
  private final List<ValidationRule> jobTypeRules;

  public JobValidatorProcessor(ApplicationProperties properties, PatternCache patternCache) {
    this.patternCache = patternCache;
    JobsHunter jobsHunter = properties.getJobsHunter();

    List<Pattern> expiredJobsPatterns = this.parseExpressions(jobsHunter.getExpiredExpressions());
    this.freelancerPattern = this.parseExpressions(jobsHunter.getFreelancerExpressions());
    this.localJobPattern = this.parseExpressions(jobsHunter.getLocalJobExpressions());
    this.remotePattern = this.parseExpressions(jobsHunter.getRemoteExpressions());

    this.notExpiredRule = new NotExpiredRule(expiredJobsPatterns);
    this.jobTypeRules = List.of(
        new LocalJobsRule(),
        new B2BJobsRule(),
        new EORJobsRule()
    );
  }

  private List<Pattern> parseExpressions(String expressions) {
    List<Pattern> result = new ArrayList<>();
    for (String keyword : expressions.split(",")) {
      result.add(patternCache.getPhrasePattern(keyword));
    }
    return Collections.unmodifiableList(result);
  }

  @Override
  public JobContext processAsync(JobContext context) {
    if (this.isValidJobSync(context)) {
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
      ValidationResult validRule = jobTypeRules.stream()
          .map(rule -> rule.validate(ctx))
          .filter(ValidationResult::isValid)
          .findFirst()
          .orElse(null);

      if (validRule != null) {
        log.info("URL is valid because [{}],  {}", validRule.reason(), url);
      } else {
        log.info("URL is valid but does not match user preferences {}", url);
      }
      return validRule != null;
    } catch (Throwable e) {
      log.error("Invalid URL. Unexpected exception: " + e.getMessage());
      return false;
    }
  }

  private ValidationContext buildValidationContext(JobContext jobContext) {
    String html = this.normalize(jobContext.getBody().toLowerCase());
    return ValidationContext.builder()
        .html(html)
        .jobContext(jobContext)
        .cityMatch(patternCache.matchesWord(jobContext.getCity(), html))
        .countryMatch(patternCache.matchesWord(jobContext.getCountry(), html))
        .freelancerRole(freelancerPattern.stream().anyMatch(p -> p.matcher(html).find()))
        .remoteRole(remotePattern.stream().anyMatch(p -> p.matcher(html).find()))
        .localJob(localJobPattern.stream().anyMatch(p -> p.matcher(html).find()))
        .build();
  }

  private String normalize(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    return text
        .replace('\u00A0', ' ')
        .replaceAll("\\s+", " ")
        .trim();
  }
}
