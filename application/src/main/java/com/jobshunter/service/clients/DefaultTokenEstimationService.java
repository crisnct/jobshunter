package com.jobshunter.service.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.dto.TokenEstimationRequest;
import com.jobshunter.dto.TokenEstimationResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class DefaultTokenEstimationService implements TokenEstimationService {

  private final ObjectMapper objectMapper;

  public DefaultTokenEstimationService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public TokenEstimationResult estimateTokens(TokenEstimationRequest request) {
    Float tokensPerChar = request.aiModel().getTokensPerChar();
    if (tokensPerChar == null) {
      throw new IllegalStateException("tokens_per_char is not configured for model " + request.aiModel().getModel());
    }

    int inputTokens = safeList(request.prompts()).stream()
        .mapToInt(value -> estimateText(value, tokensPerChar))
        .sum();

    int toolTokens = safeList(request.tools()).stream()
        .mapToInt(p -> estimateJson(p, tokensPerChar))
        .sum();

    int schemaTokens = request.responseSchema() == null ? 0 : estimateJson(request.responseSchema(), tokensPerChar);
    int outputTokens = request.maxOutputTokens();
    int totalTokens = inputTokens + toolTokens + schemaTokens + outputTokens;
    int safeLimit = (int) (request.aiModel().getContextWindow() * getSafetyRatio(request.aiModel()));

    return new TokenEstimationResult(
        inputTokens,
        toolTokens,
        schemaTokens,
        outputTokens,
        totalTokens,
        safeLimit,
        totalTokens <= safeLimit
    );
  }

  @Override
  public float getSafetyRatio(AiModelEntity model) {
    return (switch (model.getProvider()) {
      case GPT -> 0.85f;
      case GEMINI -> 0.8f;
      case GROK -> 0.75f;
      case SERP -> throw new IllegalArgumentException("Not indended to be used for SERP");
    });
  }

  private int estimateText(String text, float tokensPerChar) {
    if (StringUtils.isBlank(text)) {
      return 0;
    } else {
      return (int) Math.ceil(text.length() * tokensPerChar);
    }
  }

  private int estimateJson(Object value, float tokensPerChar) {
    try {
      String json = objectMapper.writeValueAsString(value);
      return estimateText(json, tokensPerChar);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize object for token estimation", e);
    }
  }

  private <T> java.util.List<T> safeList(java.util.List<T> list) {
    return list == null ? java.util.List.of() : list;
  }
}
