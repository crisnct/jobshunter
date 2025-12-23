package com.jobshunter.service.testdata;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.GptJobScoreCalculatorClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "false")
public final class DummyGptJobScoreCalculatorClient implements GptJobScoreCalculatorClient {

  @Override
  public int computeScore(String jobDescription, String fileId) {
    return jobDescription.charAt(0) % 15 + 85;
  }

}
