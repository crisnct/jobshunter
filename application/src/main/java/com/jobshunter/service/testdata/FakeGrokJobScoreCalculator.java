package com.jobshunter.service.testdata;

import com.jobshunter.model.GrokJobScoreRequest;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.JobScoreCalculatorClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("GrokJobScoreCalculator")
@PackageExpected("com.jobshunter.service.clients.grok")
@ConditionalOnProperty(name = "grok.enabled", havingValue = "false")
public non-sealed class FakeGrokJobScoreCalculator implements JobScoreCalculatorClient<GrokJobScoreRequest> {

  @Override
  @RateLimiter(name = "grokLimiter")
  @CircuitBreaker(name = "grokCircuitBreaker", fallbackMethod = "fallbackComputeScore")
  @Bulkhead(name = "grokBulkhead")
  public int computeScore(GrokJobScoreRequest request) {
    return request.getJobDescription().charAt(0) % 15 + 85;
  }

  @SuppressWarnings("unused")
  private int fallbackComputeScore(GrokJobScoreRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return -1;
  }

}
