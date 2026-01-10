package com.jobshunter.service.application.processors;

import com.jobshunter.model.GrokJobScoreRequest;
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
@Service("jobScoringGrok")
public non-sealed class JobScoringGrok implements JobScoring<GrokJobScoreRequest> {

  @Getter
  private final FileClient fileClient;

  private final JobScoreCalculatorClient<GrokJobScoreRequest> calculator;

  public JobScoringGrok(
      @Qualifier("Grok")
      FileClient fileClient,

      @Qualifier("GrokJobScoreCalculator")
      JobScoreCalculatorClient<GrokJobScoreRequest> calculator
  ) {
    this.calculator = calculator;
    this.fileClient = fileClient;
  }

  @Override
  public JobContext processAsync(JobContext context) {
    Job job = context.getJob();
    int score;
    if (context.isAccepted() && context.getDescription() != null) {
      log.info("Computing matching score between {} resume and description of job {}",
          context.getUser().getUsername(), job.getUrl());
      score = calculator.computeScore(
          new GrokJobScoreRequest(context.getDescription(), context.getUser().getCv()));
    } else {
      score = -1;
    }
    job.setScore(score);
    context.setPhase(JobPhase.SCORED);
    return context;
  }

}
