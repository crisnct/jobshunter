package com.jobshunter.service.testdata;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.JobScoreCalculatorClient;
import com.jobshunter.service.clients.gpt.GptJobScoreRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "false")
public final class DummyJobScoreCalculatorClient implements JobScoreCalculatorClient<GptJobScoreRequest> {

  @Override
  public int computeScore(GptJobScoreRequest request) {
    return request.getJobDescription().charAt(0) % 15 + 85;
  }

}
