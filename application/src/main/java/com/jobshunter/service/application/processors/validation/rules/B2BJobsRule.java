package com.jobshunter.service.application.processors.validation.rules;

import com.jobshunter.model.ContractType;
import com.jobshunter.service.application.processors.validation.ValidationContext;
import com.jobshunter.service.application.processors.validation.ValidationResult;
import com.jobshunter.service.application.processors.validation.ValidationRule;

/**
 * Validates B2B contract jobs that are remote. Requires freelancer role indication and remote work indication.
 */
public class B2BJobsRule implements ValidationRule {

  @Override
  public ValidationResult validate(ValidationContext context) {
    boolean userWantsB2B = context.getUserContractTypes().stream()
        .anyMatch(type -> type == ContractType.B2B);
    if (userWantsB2B
        && context.isFreelancerRole()
        && (context.isCityMatch() || context.isCountryMatch() || !context.isLocalJob())) {
      return ValidationResult.success("B2B freelancer remote job");
    }
    return ValidationResult.failure("No B2B contract, freelancer role, or remote indication");
  }
}
