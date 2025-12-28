package com.jobshunter.service.testdata;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.JobScoreCalculatorClient;
import com.jobshunter.service.clients.gemini.GeminiJobScoreRequest;
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
public non-sealed class DummyGeminiJobScoreCalculator implements JobScoreCalculatorClient<GeminiJobScoreRequest> {

  @Override
  @RateLimiter(name = "geminiLimiter")
  public int computeScore(GeminiJobScoreRequest request) {
    return request.getResumeFileId().charAt(0) % 15 + 85;
  }

}
