package com.jobshunter.service.testdata;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.FileClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("Gemini")
@PackageExpected("com.jobshunter.service.clients.gemini")
@ConditionalOnProperty(name = "gemini.enabled", havingValue = "false")
public non-sealed class FakeGeminiFileApiClient implements FileClient {

  @Override
  @RateLimiter(name = "geminiLimiter")
  @CircuitBreaker(name = "geminiCircuitBreaker", fallbackMethod = "fallbackUploadFile")
  public String uploadFile(Path cvPath) {
    log.info("File {} uploaded properly", cvPath);
    return "uploaded";
  }

  @Override
  public void deleteFile(String fileId) {
    log.info("File {} deleted", fileId);
  }

  @Override
  public void deleteAllFilesExcept(List<String> fileIds) {
    log.info("Files unused deleted");
  }

  @SuppressWarnings("unused")
  private String fallbackUploadFile(Path cvPath, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return "";
  }

}
