package com.jobshunter.dto;

/**
 * Usage reported by an AI provider for one request.
 *
 * @param reportedCostUsd exact cost in USD when the provider computes it itself (Perplexity), otherwise null and the cost is derived from the
 *                        prices stored in {@code ai_models}
 */
public record TokensConsumed(
    int inputTokens,
    int outputTokens,
    int toolCalls,
    Double reportedCostUsd
) {

  public TokensConsumed(int inputTokens, int outputTokens, int toolCalls) {
    this(inputTokens, outputTokens, toolCalls, null);
  }
}
