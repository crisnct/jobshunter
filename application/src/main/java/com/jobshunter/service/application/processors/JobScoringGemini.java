package com.jobshunter.service.application.processors;

import com.jobshunter.model.EngineType;
import com.jobshunter.model.GeminiJobScoreRequest;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobPhase;
import com.jobshunter.service.clients.FileClient;
import com.jobshunter.service.clients.JobScoreCalculatorClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service("jobScoringGemini")
public non-sealed class JobScoringGemini implements JobScoring<GeminiJobScoreRequest> {

  @Getter
  private final FileClient fileClient;

  private final JobScoreCalculatorClient<GeminiJobScoreRequest> calculator;

  public JobScoringGemini(
      @Qualifier("Gemini")
      FileClient fileClient,

      @Qualifier("GeminiJobScoreCalculator")
      JobScoreCalculatorClient<GeminiJobScoreRequest> calculator
  ) {
    this.calculator = calculator;
    this.fileClient = fileClient;
  }

  @Override
  public JobContext processAsync(JobContext context) {
    Job job = context.getJob();
    int score = 0;
    if (context.isValidatedSuccessfully() && context.getDescription() != null) {
      String fileId = context.getUser().getRemoteCvs().stream().filter(p -> p.getProvider() == EngineType.GEMINI)
          .findFirst().orElseThrow().getFileId();
      log.info("Computing matching score between {} resume and description of job {}",
          context.getUser().getUsername(), job.getUrl());
      score = calculator.computeScore(
          new GeminiJobScoreRequest(fileId, context.getDescription()));
    }
    job.setScore(score);
    context.setPhase(JobPhase.SCORING);
    return context;
  }

  private Path createPathFromByteArray(byte[] bytearray, String prefix) throws IOException {
    Path pdfPath = Files.createTempFile(prefix + "-", ".txt");
    Files.write(pdfPath, bytearray, StandardOpenOption.TRUNCATE_EXISTING);
    pdfPath.toFile().deleteOnExit();
    return pdfPath;
  }

}
