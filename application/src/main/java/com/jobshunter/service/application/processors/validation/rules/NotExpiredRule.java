package com.jobshunter.service.application.processors.validation.rules;

import com.jobshunter.service.application.processors.validation.ValidationContext;
import com.jobshunter.service.application.processors.validation.ValidationResult;
import com.jobshunter.service.application.processors.validation.ValidationRule;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates that the job posting is not expired by checking for expired job patterns.
 */
public class NotExpiredRule implements ValidationRule {

  private final List<Pattern> expiredJobsPatterns;

  public NotExpiredRule(List<Pattern> expiredJobsPatterns) {
    this.expiredJobsPatterns = expiredJobsPatterns;
  }

  @Override
  public ValidationResult validate(ValidationContext context) {
    String html = context.getHtml();
    boolean hasExpiredPattern = expiredJobsPatterns.stream()
        .anyMatch(pattern -> pattern.matcher(html).find());

    if (hasExpiredPattern) {
      return ValidationResult.failure("Job posting appears to be expired");
    }
    return ValidationResult.success();
  }
}
