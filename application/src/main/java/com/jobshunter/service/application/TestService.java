package com.jobshunter.service.application;

import com.jobshunter.dto.exceptions.ValidationException;
import com.jobshunter.model.EngineType;
import com.jobshunter.service.clients.JobScoreCalculatorClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TestService {

  private final JobScoreCalculatorClient gptJobScoreCalculator;

  private final JobScoreCalculatorClient geminiJobScoreCalculator;

  private final JobScoreCalculatorClient grokJobScoreCalculator;

  public TestService(
      @Qualifier("GptJobScoreCalculator") JobScoreCalculatorClient gptJobScoreCalculator,
      @Qualifier("GeminiJobScoreCalculator") JobScoreCalculatorClient geminiJobScoreCalculator,
      @Qualifier("GrokJobScoreCalculator") JobScoreCalculatorClient grokJobScoreCalculator
  ) {
    this.gptJobScoreCalculator = gptJobScoreCalculator;
    this.geminiJobScoreCalculator = geminiJobScoreCalculator;
    this.grokJobScoreCalculator = grokJobScoreCalculator;
  }

  public JobScoreCalculatorClient getScoreCalculator(EngineType type) {
    return (switch (type) {
      case GPT -> gptJobScoreCalculator;
      case GEMINI -> geminiJobScoreCalculator;
      case GROK -> grokJobScoreCalculator;
      default -> throw new ValidationException("Invalid engine provider. Must be GPT, GEMINI, or GROK");
    });
  }

}
