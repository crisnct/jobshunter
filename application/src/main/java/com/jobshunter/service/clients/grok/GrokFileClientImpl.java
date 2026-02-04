package com.jobshunter.service.clients.grok;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.dto.grokResponse.FileInfo;
import com.jobshunter.dto.grokResponse.FileListResponse;
import com.jobshunter.model.ResumeFileInfo;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.FileClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("Grok")
@PackageExpected("com.jobshunter.service.clients.grok")
@ConditionalOnProperty(name = "grok.enabled", havingValue = "true")
@RequiredArgsConstructor
public non-sealed class GrokFileClientImpl implements FileClient {

  private static final String API_URI = "https://api.x.ai/v1/files";

  private final RestClient restClient;

  private final ApplicationProperties properties;

  @Override
  @CircuitBreaker(name = "grokCircuitBreaker", fallbackMethod = "fallbackUploadFile")
  @Bulkhead(name = "grokBulkhead")
  @RateLimiter(name = "grokLimiter")
  public ResumeFileInfo uploadFile(Path cvPath) throws IOException {
    log.info("Uploading file to GROK {}...", cvPath.getFileName());
    try (var ignored = Files.newInputStream(cvPath)) {
      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("file", new FileSystemResource(cvPath));

      UploadFileResponse response = restClient.post()
          .uri(URI.create(API_URI))
          .headers((h) -> h.setBearerAuth(properties.getGrok().getApiKey()))
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(body)
          .retrieve()
          .body(UploadFileResponse.class);

      if (response == null || Strings.isBlank(response.id())) {
        throw new RuntimeException("Fail to upload file to GROK Api: " + cvPath);
      }
      return new ResumeFileInfo(response.id(), response.filename(), response.expires_at());
    }
  }

  @Override
  @Bulkhead(name = "grokBulkhead")
  @RateLimiter(name = "grokLimiter")
  public void deleteFile(@NotBlank String fileId) {
    String body = restClient.delete()
        .uri(API_URI + "/" + fileId)
        .headers((h) -> h.setBearerAuth(properties.getGrok().getApiKey()))
        .retrieve()
        .body(String.class);
    log.info("grok Deleted fileId:{} with bodyResponse: {}", fileId, body);
  }

  @Override
  public void deleteAllFilesExcept(@NotBlank List<String> fileIds) {
    FileListResponse response = restClient.get()
        .uri(API_URI)
        .headers((h) -> h.setBearerAuth(properties.getGrok().getApiKey()))
        .retrieve()
        .body(FileListResponse.class);
    if (response != null && response.data() != null) {
      List<FileInfo> toDelete = response.data().stream().filter(f -> !fileIds.contains(f.id())).toList();
      for (FileInfo file : toDelete) {
        deleteFile(file.id());
      }
      log.info("Deleted {} files from GROK", toDelete.size());
    }
  }

  @SuppressWarnings("unused")
  private ResumeFileInfo fallbackUploadFile(Path cvPath, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return null;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record UploadFileResponse(String id, String filename, Instant expires_at) {

  }

}
