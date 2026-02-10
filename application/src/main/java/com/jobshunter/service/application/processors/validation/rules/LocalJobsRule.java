package com.jobshunter.service.application.processors.validation.rules;

import com.jobshunter.service.application.processors.validation.ValidationContext;
import com.jobshunter.service.application.processors.validation.ValidationResult;
import com.jobshunter.service.application.processors.validation.ValidationRule;

/**
 * Validates jobs for users seeking ONSITE or HYBRID positions. Requires a city match in the job posting.
 */
public class LocalJobsRule implements ValidationRule {

  @Override
  public ValidationResult validate(ValidationContext context) {
    if (context.isCityMatch()) {
      return ValidationResult.success("Local job, city match");
    }
    return ValidationResult.failure("No city match");
  }
}
