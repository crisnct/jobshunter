package com.jobshunter.service.application.processors;

import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobPhase;
import com.jobshunter.service.clients.FileClient;
import com.jobshunter.service.clients.JobScoreCalculatorClient;
import com.jobshunter.model.GeminiJobScoreRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobScoring implements JobProcessor {

  @Getter
  private final FileClient fileClient;

  private final JobScoreCalculatorClient<GeminiJobScoreRequest> calculator;

  public JobScoring(
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
    int score;
    if (context.isAccepted()) {
      try {
        Path path = createPathFromByteArray(job.getDescription().getBytes(StandardCharsets.UTF_8),
            "jd-user-" + context.getUser().getUsername(), ".txt");
        try (var uploadedFile = new UploadedFile(fileClient, path)) {
          log.info("Computing matching score between {} resume and description of job {}",
              context.getUser().getUsername(), StringUtils.abbreviate(job.getUrl(), 50));
          score = calculator.computeScore(
              new GeminiJobScoreRequest(context.getResumeFileId(), uploadedFile.getFileInfo().fileId()));
        }
      } catch (IOException e) {
        throw new RuntimeException("Unexpected error about creating file on local storage at scoring job  " + job, e);
      }
    } else {
      score = -1;
    }
    job.setScore(score);
    context.setPhase(JobPhase.SCORED);
    return context;
  }

  public String uploadUserCv(UserCvEntity cv) throws IOException {
    Path pdfPath = createPathFromByteArray(cv.getCv(), "resume-" + cv.getUser().getUsername(), ".pdf");
    return fileClient.uploadFile(pdfPath).fileId();
  }

  private Path createPathFromByteArray(byte[] bytearray, String prefix, String extension) throws IOException {
    Path pdfPath = Files.createTempFile(prefix + "-", extension);
    Files.write(pdfPath, bytearray, StandardOpenOption.TRUNCATE_EXISTING);
    pdfPath.toFile().deleteOnExit();
    return pdfPath;
  }

  public void cleanup(String resumeFileId) {
    fileClient.deleteFile(resumeFileId);
  }

}
