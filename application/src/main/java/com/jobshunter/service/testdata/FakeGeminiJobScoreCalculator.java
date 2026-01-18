package com.jobshunter.service.testdata;

import com.jobshunter.model.JobScoreRequest;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.JobScoreCalculatorClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("GeminiJobScoreCalculator")
@PackageExpected("com.jobshunter.service.clients.gemini")
@ConditionalOnProperty(name = "gemini.enabled", havingValue = "false")
@RequiredArgsConstructor
public non-sealed class FakeGeminiJobScoreCalculator implements JobScoreCalculatorClient {

  @Override
  @RateLimiter(name = "geminiLimiter")
  @CircuitBreaker(name = "geminiCircuitBreaker", fallbackMethod = "fallbackComputeScore")
  @Bulkhead(name = "geminiBulkhead")
  public int computeScore(JobScoreRequest request) {
    return request.getJobDescription().charAt(0) % 15 + 85;
  }

  @SuppressWarnings("unused")
  private int fallbackComputeScore(JobScoreRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return -1;
  }

}
