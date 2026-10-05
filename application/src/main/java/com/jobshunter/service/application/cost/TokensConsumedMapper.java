package com.jobshunter.service.application.cost;

import com.jobshunter.dto.TokensConsumed;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse.UsageMetadata;
import com.jobshunter.dto.gptResponse.Usage;
import java.util.Objects;

/**
 * Maps usage objects from GPT, Gemini, Grok and Perplexity API responses to TokensConsumed.
 */
public final class TokensConsumedMapper {

  private TokensConsumedMapper() {
  }

  public static TokensConsumed fromGpt(Usage usage) {
    if (usage == null) {
      return new TokensConsumed(0, 0, 0);
    }
    return new TokensConsumed(usage.inputTokens(), usage.outputTokens(), 0);
  }

  public static TokensConsumed fromGemini(UsageMetadata usage) {
    if (usage == null) {
      return new TokensConsumed(0, 0, 0);
    }
    int input = Objects.requireNonNullElse(usage.promptTokenCount(), 0);
    int output = Objects.requireNonNullElse(usage.candidatesTokenCount(), 0);
    return new TokensConsumed(input, output, 0);
  }

  public static TokensConsumed fromGrok(com.jobshunter.dto.grokResponse.Usage usage) {
    if (usage == null) {
      return new TokensConsumed(0, 0, 0);
    }
    int toolCalls = Objects.requireNonNullElse(usage.numServerSideToolsUsed(), 0);
    return new TokensConsumed(usage.inputTokens(), usage.outputTokens(), toolCalls);
  }

  public static TokensConsumed fromPerplexity(com.jobshunter.dto.perplexityResponse.Usage usage) {
    if (usage == null) {
      return new TokensConsumed(0, 0, 0);
    }
    Double reportedCost = usage.cost() == null ? null : usage.cost().totalCost();
    return new TokensConsumed(usage.inputTokens(), usage.outputTokens(), usage.toolInvocations(), reportedCost);
  }
}
