package com.jobshunter.service.application.processors;

import com.jobshunter.dto.gptRequest.Reasoning;
import com.jobshunter.model.GptJobScoreRequest;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobPhase;
import com.jobshunter.service.clients.FileClient;
import com.jobshunter.service.clients.JobScoreCalculatorClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service("jobScoringGpt")
public non-sealed class JobScoringGpt implements JobScoring<GptJobScoreRequest> {

  @Getter
  private final FileClient fileClient;

  private final JobScoreCalculatorClient<GptJobScoreRequest> calculator;

  public JobScoringGpt(
      @Qualifier("Gpt")
      FileClient fileClient,

      @Qualifier("GptJobScoreCalculator")
      JobScoreCalculatorClient<GptJobScoreRequest> calculator
  ) {
    this.calculator = calculator;
    this.fileClient = fileClient;
  }

  @Override
  public JobContext processAsync(JobContext context) {
    Job job = context.getJob();
    int score;
    if (context.isValidatedSuccessfully() && context.getDescription() != null) {
      log.info("Computing matching score between {} resume and description of job {}",
          context.getUser().getUsername(), job.getUrl());
      GptJobScoreRequest request
          = new GptJobScoreRequest(context.getDescription(), context.getUser().getCv(), new Reasoning("none"));
      score = calculator.computeScore(request);
    } else {
      score = -1;
    }
    job.setScore(score);
    context.setPhase(JobPhase.SCORING);
    return context;
  }

}
