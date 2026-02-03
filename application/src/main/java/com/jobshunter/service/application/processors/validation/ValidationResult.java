package com.jobshunter.service.application.processors.validation;

/**
 * Result of a validation rule execution.
 */
public record ValidationResult(boolean valid, String reason) {

  public static ValidationResult success() {
    return new ValidationResult(true, null);
  }

  public static ValidationResult success(String reason) {
    return new ValidationResult(true, reason);
  }

  public static ValidationResult failure(String reason) {
    return new ValidationResult(false, reason);
  }

  public boolean isValid() {
    return this.valid;
  }
}
