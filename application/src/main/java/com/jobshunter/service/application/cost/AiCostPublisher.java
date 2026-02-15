package com.jobshunter.service.application.cost;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.dto.TokenEstimationResult;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse.UsageMetadata;
import com.jobshunter.dto.gptResponse.Usage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Service for publishing AI API cost events. Centralizes cost event publishing logic that was previously duplicated across AI clients.
 */
@Service
@RequiredArgsConstructor
public class AiCostPublisher {

  private final ApplicationEventPublisher eventPublisher;

  /**
   * Publishes cost event for GPT API calls.
   *
   * @param orderId the job order ID
   * @param model   the AI model used
   * @param usage   the GPT usage data (can be null)
   */
  public void publishGpt(Long orderId, AiModelEntity model, TokenEstimationResult estmTokens, Usage usage) {
    if (usage != null) {
      eventPublisher.publishEvent(new AiRequestCostEvent(this, orderId, model, estmTokens, TokensConsumedMapper.fromGpt(usage)));
    }
  }

  /**
   * Publishes cost event for Gemini API calls.
   *
   * @param orderId the job order ID
   * @param model   the AI model used
   * @param usage   the Gemini usage metadata (can be null)
   */
  public void publishGemini(Long orderId, AiModelEntity model, TokenEstimationResult estmTokens, UsageMetadata usage) {
    if (usage != null) {
      eventPublisher.publishEvent(new AiRequestCostEvent(this, orderId, model, estmTokens, TokensConsumedMapper.fromGemini(usage)));
    }
  }

  /**
   * Publishes cost event for Grok API calls.
   *
   * @param orderId the job order ID
   * @param model   the AI model used
   * @param usage   the Grok usage data (can be null)
   */
  public void publishGrok(Long orderId, AiModelEntity model, TokenEstimationResult estmTokens, com.jobshunter.dto.grokResponse.Usage usage) {
    if (usage != null) {
      eventPublisher.publishEvent(new AiRequestCostEvent(this, orderId, model, estmTokens, TokensConsumedMapper.fromGrok(usage)));
    }
  }
}
