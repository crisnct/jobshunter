package com.jobshunter.service.clients.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.dto.geminiResponse.FileInfo;
import com.jobshunter.dto.geminiResponse.FileListResponse;
import com.jobshunter.model.ResumeFileInfo;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.FileClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("Gemini")
@PackageExpected("com.jobshunter.service.clients.gemini")
@ConditionalOnProperty(name = "gemini.enabled", havingValue = "true")
@RequiredArgsConstructor
public non-sealed class GeminiFileClientImpl implements FileClient {

  public static final String UPLOAD_URI = "https://generativelanguage.googleapis.com/upload/v1beta/files";

  private static final String DELETE_URI = "https://generativelanguage.googleapis.com/v1beta";

  private static final String GET_FILES_URI = "https://generativelanguage.googleapis.com/v1beta/files?key=";

  private final RestClient restClient;

  private final ApplicationProperties properties;

  @Override
  @CircuitBreaker(name = "geminiCircuitBreaker", fallbackMethod = "fallbackUploadFile")
  @Bulkhead(name = "geminiBulkhead")
  @RateLimiter(name = "geminiLimiter")
  public ResumeFileInfo uploadFile(Path cvPath) throws IOException {
    log.info("Uploading file to GEMINI {}...", cvPath.getFileName());
    try (var ignored = Files.newInputStream(cvPath)) {
      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

      HttpHeaders jsonHeaders = new HttpHeaders();
      jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

      final String mimeType;
      if (cvPath.endsWith("pdf")) {
        mimeType = MediaType.APPLICATION_PDF_VALUE;
      } else if (cvPath.endsWith("txt") || cvPath.endsWith("html") || cvPath.endsWith("htm")) {
        mimeType = MediaType.TEXT_HTML_VALUE;
      } else {
        mimeType = MediaType.ALL_VALUE;
      }

      String metadata = String.format("""
          {
              "file": {
                "displayName": "%s",
                "mimeType": "%s"
              }
           }
          """, cvPath.getFileName(), mimeType);
      body.add("metadata", new HttpEntity<>(metadata, jsonHeaders));
      body.add("file", new FileSystemResource(cvPath));

      UploadFileResponse response = restClient.post()
          .uri(UPLOAD_URI + "?key=" + properties.getGemini().getApiKey())
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(body)
          .retrieve()
          .body(UploadFileResponse.class);

      if (response == null || response.file() == null) {
        throw new RuntimeException("Fail to upload file to Gemini Api: " + cvPath);
      }
      return new ResumeFileInfo(response.file().name, response.file().displayName, response.file.expirationTime);
    }
  }

  @Override
  @Bulkhead(name = "geminiBulkhead")
  @RateLimiter(name = "geminiLimiter")
  public void deleteFile(@NotBlank String fileId) {
    String body = restClient.delete()
        .uri(DELETE_URI + "/" + fileId + "?key=" + properties.getGemini().getApiKey())
        .retrieve()
        .body(String.class);
    log.info("gemini Deleted fileId:{} with bodyResponse: {}", fileId, body);
  }

  @Override
  public void deleteAllFilesExcept(List<String> fileIds) {
    FileListResponse response = restClient.get()
        .uri(GET_FILES_URI + properties.getGemini().getApiKey())
        .retrieve()
        .body(FileListResponse.class);
    if (response != null && response.files() != null) {
      List<FileInfo> toDelete = response.files().stream().filter(f -> !fileIds.contains(f.name())).toList();
      for (FileInfo file : toDelete) {
        deleteFile(file.name());
      }
      log.info("Deleted {} files from GEMINI", toDelete.size());
    }
  }

  @SuppressWarnings("unused")
  private ResumeFileInfo fallbackUploadFile(Path cvPath, Throwable t) {
    log.error("{} call short-circuited/bulkheaded fallbackUploadFile: {}", getClass().getSimpleName(), t.getMessage(), t);
    return null;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record UploadFileResponse(FileResponse file) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FileResponse(String name, String displayName, Instant expirationTime) {

  }

}
