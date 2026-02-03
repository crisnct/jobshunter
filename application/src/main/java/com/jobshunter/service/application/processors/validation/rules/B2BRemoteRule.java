package com.jobshunter.service.application.processors.validation.rules;

import com.jobshunter.model.ContractType;
import com.jobshunter.service.application.processors.validation.ValidationContext;
import com.jobshunter.service.application.processors.validation.ValidationResult;
import com.jobshunter.service.application.processors.validation.ValidationRule;

/**
 * Validates B2B contract jobs that are remote.
 * Requires freelancer role indication and remote work indication.
 */
public class B2BRemoteRule implements ValidationRule {

  @Override
  public ValidationResult validate(ValidationContext context) {
    boolean hasB2B = context.getUserContractTypes().stream()
        .anyMatch(type -> type == ContractType.B2B);

    if (hasB2B && context.isFreelancerRole() && context.isRemoteRole()) {
      return ValidationResult.success("B2B freelancer remote job");
    }
    return ValidationResult.failure("No B2B contract, freelancer role, or remote indication");
  }
}
