package com.jobshunter.service.application.processors.validation;

/**
 * Interface for job validation rules.
 * Each implementation represents a specific validation criterion.
 */
public interface ValidationRule {

  /**
   * Validates the job context against this rule.
   *
   * @param context the validation context with pre-calculated data
   * @return the validation result indicating if the rule passed
   */
  ValidationResult validate(ValidationContext context);

  /**
   * Returns the name of this rule for logging purposes.
   */
  default String getRuleName() {
    return this.getClass().getSimpleName();
  }
}
