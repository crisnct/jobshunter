package com.jobshunter.service.application.processors.validation.rules;

import com.jobshunter.model.ContractType;
import com.jobshunter.service.application.processors.validation.ValidationContext;
import com.jobshunter.service.application.processors.validation.ValidationResult;
import com.jobshunter.service.application.processors.validation.ValidationRule;

/**
 * Validates employment-type jobs (EMPLOYMENT, EOR, INTERNSHIP) that require local presence.
 * Requires a city match in the job posting.
 */
public class EmploymentLocalRule implements ValidationRule {

  @Override
  public ValidationResult validate(ValidationContext context) {
    boolean hasEmploymentType = context.getUserContractTypes().stream()
        .anyMatch(type -> type == ContractType.EMPLOYMENT
            || type == ContractType.EOR
            || type == ContractType.INTERNSHIP);

    if (hasEmploymentType && context.isCityMatch()) {
      return ValidationResult.success("Employment/EOR/Internship job with city match");
    }
    return ValidationResult.failure("No employment contract type or city mismatch");
  }
}
