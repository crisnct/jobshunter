package com.jobshunter.service.application.processors.validation.rules;

import com.jobshunter.model.ContractType;
import com.jobshunter.service.application.processors.validation.ValidationContext;
import com.jobshunter.service.application.processors.validation.ValidationResult;
import com.jobshunter.service.application.processors.validation.ValidationRule;

/**
 * Validates employment-type jobs (EMPLOYMENT, EOR, INTERNSHIP) that require local presence. Requires a city match in the job posting.
 */
public class EORJobsRule implements ValidationRule {

  @Override
  public ValidationResult validate(ValidationContext context) {
    boolean userWantsEor = context.getUserContractTypes().stream()
        .anyMatch(type -> type == ContractType.EOR);

    if (userWantsEor
        && context.isRemoteRole()
        && !context.isFreelancerRole()
        && (context.isCityMatch() || context.isCountryMatch() || !context.isLocalJob())) {
      return ValidationResult.success("EOR remote job");
    }
    return ValidationResult.failure("No EOR job found");
  }
}
