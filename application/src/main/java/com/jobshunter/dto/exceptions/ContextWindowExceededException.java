package com.jobshunter.dto.exceptions;

public class ContextWindowExceededException extends RuntimeException {

  public ContextWindowExceededException(String modelId, int estimatedTokens, int safeLimit) {
    super(
        "Context window exceeded for model [" + modelId + "] " +
            "(estimated=" + estimatedTokens +
            ", safeLimit=" + safeLimit + ")"
    );
  }
}
