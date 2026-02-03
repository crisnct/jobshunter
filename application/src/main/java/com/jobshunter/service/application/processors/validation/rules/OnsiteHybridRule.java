package com.jobshunter.service.application.processors.validation.rules;

import com.jobshunter.model.JobType;
import com.jobshunter.service.application.processors.validation.ValidationContext;
import com.jobshunter.service.application.processors.validation.ValidationResult;
import com.jobshunter.service.application.processors.validation.ValidationRule;

/**
 * Validates jobs for users seeking ONSITE or HYBRID positions.
 * Requires a city match in the job posting.
 */
public class OnsiteHybridRule implements ValidationRule {

  @Override
  public ValidationResult validate(ValidationContext context) {
    boolean hasOnsiteOrHybrid = context.getUserJobTypes().stream()
        .anyMatch(type -> type == JobType.ONSITE || type == JobType.HYBRID);

    if (hasOnsiteOrHybrid && context.isCityMatch()) {
      return ValidationResult.success("Onsite/Hybrid job with city match");
    }
    return ValidationResult.failure("No onsite/hybrid job type or city mismatch");
  }
}
