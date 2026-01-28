package com.jobshunter.service.clients;

import com.jobshunter.dto.TokenEstimationRequest;
import com.jobshunter.dto.TokenEstimationResult;
import com.jobshunter.dto.exceptions.ContextWindowExceededException;
import com.jobshunter.dto.geminiRequest.GeminiJobsPayload;
import com.jobshunter.dto.gptRequest.GptJobsPayload;
import com.jobshunter.dto.grokRequest.GrokJobsPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Central guard to validate token usage against model context windows. Keeps payload builders lean while enforcing a single estimation path.
 */
@SuppressWarnings("UnusedReturnValue")
@Component
@RequiredArgsConstructor
public class TokenEstimationGuard {

  private final TokenEstimationService tokenEstimationService;

  public TokenEstimationResult assertFitsContext(GptJobsPayload payload) {
    return assertFitsContext(TokenEstimationMapper.from((payload)));
  }

  public TokenEstimationResult assertFitsContext(GeminiJobsPayload payload) {
    return assertFitsContext(TokenEstimationMapper.from((payload)));
  }

  public TokenEstimationResult assertFitsContext(GrokJobsPayload payload) {
    return assertFitsContext(TokenEstimationMapper.from((payload)));
  }

  private TokenEstimationResult assertFitsContext(TokenEstimationRequest request) {
    TokenEstimationResult result = tokenEstimationService.estimateTokens(request);
    if (!result.fitsContextWindow()) {
      throw new ContextWindowExceededException(
          request.aiModel().getModel(),
          result.estimatedTotalTokens(),
          result.safeContextLimit()
      );
    }
    return result;
  }

}
