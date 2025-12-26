package com.jobshunter.service.application;

import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.model.Job;
import com.jobshunter.service.clients.FileClient;
import com.jobshunter.service.clients.JobScoreCalculatorClient;
import com.jobshunter.service.clients.gemini.GeminiJobScoreRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobScoring {

  private final Executor executor;

  private final FileClient fileClient;

  private final JobScoreCalculatorClient<GeminiJobScoreRequest> calculator;

  public JobScoring(
      @Qualifier("geminiSearchExecutor")
      Executor executor,

      @Qualifier("Gemini")
      FileClient fileClient,

      @Qualifier("GeminiScoreCalculator")
      JobScoreCalculatorClient<GeminiJobScoreRequest> calculator
  ) {
    this.executor = executor;
    this.calculator = calculator;
    this.fileClient = fileClient;
  }

  public CompletableFuture<Void> calculateScore(Job job, UserEntity user, String resumeFileId) {
    return CompletableFuture.runAsync(() -> {
      try {
        calculateScoreSync(job, user, resumeFileId);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }, executor);
  }

  public String uploadUserCv(UserCvEntity cv) throws IOException {
    Path pdfPath = createPathFromByteArray(cv.getCv(), "resume-" + cv.getUser().getUsername(), ".pdf");
    return fileClient.uploadFile(pdfPath);
  }

  private Path createPathFromByteArray(byte[] bytearray, String prefix, String extension) throws IOException {
    Path pdfPath = Files.createTempFile(prefix + "-", extension);
    Files.write(pdfPath, bytearray, StandardOpenOption.TRUNCATE_EXISTING);
    pdfPath.toFile().deleteOnExit();
    return pdfPath;
  }

  private void calculateScoreSync(Job job, UserEntity user, String resumeFileId) throws IOException {
    Path path = createPathFromByteArray(job.getDescription().getBytes(StandardCharsets.UTF_8),
        "jd-user-" + user.getUsername(), ".txt");
    String jdFileId = fileClient.uploadFile(path);
    try {
      int score = calculator.computeScore(new GeminiJobScoreRequest(resumeFileId, jdFileId));
      job.setScore(score);
    } finally {
      fileClient.deleteFile(jdFileId);
    }
  }

  public void cleanup(String resumeFileId) {
    fileClient.deleteFile(resumeFileId);
  }

}
