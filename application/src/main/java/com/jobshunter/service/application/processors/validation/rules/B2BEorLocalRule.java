package com.jobshunter.service.application.processors.validation.rules;

import com.jobshunter.model.ContractType;
import com.jobshunter.service.application.processors.validation.ValidationContext;
import com.jobshunter.service.application.processors.validation.ValidationResult;
import com.jobshunter.service.application.processors.validation.ValidationRule;

/**
 * Validates B2B/EOR contract jobs that require local presence (city or country match).
 * Requires freelancer role indication and location match.
 */
public class B2BEorLocalRule implements ValidationRule {

  @Override
  public ValidationResult validate(ValidationContext context) {
    boolean hasB2BOrEor = context.getUserContractTypes().stream()
        .anyMatch(type -> type == ContractType.B2B || type == ContractType.EOR);

    boolean hasLocationMatch = context.isCityMatch() || context.isCountryMatch();

    if (hasB2BOrEor && context.isFreelancerRole() && hasLocationMatch) {
      return ValidationResult.success("B2B/EOR freelancer job with location match");
    }
    return ValidationResult.failure("No B2B/EOR contract, freelancer role, or location match");
  }
}
