package com.jobshunter.service.testdata;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.JobScoreCalculatorClient;
import com.jobshunter.model.GptJobScoreRequest;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("GptJobScoreCalculator")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "false")
public non-sealed class FakeGptJobScoreCalculator implements JobScoreCalculatorClient<GptJobScoreRequest> {

  @Override
  @RateLimiter(name = "gptLimiter")
  public int computeScore(GptJobScoreRequest request) {
    return request.getJobDescription().charAt(0) % 15 + 85;
  }

}
