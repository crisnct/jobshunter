package com.jobshunter.service.clients;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.dto.TokenEstimationRequest;
import com.jobshunter.dto.TokenEstimationResult;

/**
 * Estimates token usage for an AI request in a conservative, provider-agnostic way. Intended for pre-flight validation against model context_window.
 */
public interface TokenEstimationService {

  TokenEstimationResult estimateTokens(TokenEstimationRequest request);

  float getSafetyRatio(AiModelEntity model);

}
