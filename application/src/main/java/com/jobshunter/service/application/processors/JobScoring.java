package com.jobshunter.service.application.processors;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.service.ModelsDBService;
import com.jobshunter.model.EngineSelection;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobPhase;
import com.jobshunter.model.JobScoreRequest;
import com.jobshunter.service.clients.JobScoreCalculatorClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobScoring implements JobProcessor {

  public static final EngineSelection ENGINE_SELECTION = new EngineSelection(EngineType.GPT, "gpt-5-nano");
  //private static final EngineSelection ENGINE_SELECTION =  new EngineSelection(EngineType.GROK, "grok-4-1-fast-reasoning");
  //private static final EngineSelection ENGINE_SELECTION =  new EngineSelection(EngineType.GEMINI, "gemini-2.0-flash-lite");

  private final JobScoreCalculatorClient gptCalculator;
  private final JobScoreCalculatorClient geminiCalculator;
  private final JobScoreCalculatorClient grokCalculator;

  private AiModelEntity aiModel;
  private final ModelsDBService modelsDBService;

  public JobScoring(
      ModelsDBService modelsDBService,

      @Qualifier("GptJobScoreCalculator")
      JobScoreCalculatorClient gptCalculator,
      @Qualifier("GeminiJobScoreCalculator")
      JobScoreCalculatorClient geminiCalculator,
      @Qualifier("GrokJobScoreCalculator")
      JobScoreCalculatorClient grokCalculator
  ) {
    this.modelsDBService = modelsDBService;
    this.gptCalculator = gptCalculator;
    this.geminiCalculator = geminiCalculator;
    this.grokCalculator = grokCalculator;
  }

  @PostConstruct
  private void init() {
    this.aiModel = modelsDBService.getModel(ENGINE_SELECTION.type(), ENGINE_SELECTION.model()).orElseThrow();
    log.info("JobScoring initialized with model: {} from {}", ENGINE_SELECTION.model(), ENGINE_SELECTION.type());
  }

  @Override
  public JobContext processAsync(JobContext context) {
    Job job = context.getJob();
    int score;
    if (context.isValidatedSuccessfully() && context.getDescription() != null) {
      log.info("Computing matching score between {} resume and description of job {}",
          context.getUser().getUsername(), job.getUrl());
      JobScoreRequest request = new JobScoreRequest(aiModel, context.getDescription(), context.getUser().getCv());
      JobScoreCalculatorClient calculator = (switch (ENGINE_SELECTION.type()) {
        case GPT -> gptCalculator;
        case GEMINI -> geminiCalculator;
        case GROK -> grokCalculator;
        default -> throw new IllegalStateException("Unexpected value: " + ENGINE_SELECTION.type());
      });
      score = calculator.computeScore(request);
    } else {
      score = -1;
    }
    job.setScore(score);
    context.setPhase(JobPhase.SCORING);
    return context;
  }

}
