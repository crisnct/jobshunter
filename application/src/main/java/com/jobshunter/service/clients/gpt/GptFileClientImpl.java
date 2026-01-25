package com.jobshunter.service.clients.gpt;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.dto.exceptions.BusinessException;
import com.jobshunter.dto.gptResponse.FileInfo;
import com.jobshunter.dto.gptResponse.FileListResponse;
import com.jobshunter.dto.gptResponse.UploadFileResponse;
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("Gpt")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "true")
@RequiredArgsConstructor
public non-sealed class GptFileClientImpl implements FileClient {

  private static final String API_URI = "https://api.openai.com/v1/files";

  private final RestClient restClient;
  private final ApplicationProperties properties;

  @Override
  @CircuitBreaker(name = "gptCircuitBreaker", fallbackMethod = "fallbackUploadFile")
  @Bulkhead(name = "gptBulkhead")
  @RateLimiter(name = "gptLimiter")
  public ResumeFileInfo uploadFile(Path cvPath) throws IOException {
    log.info("Uploading file to GPT {}...", cvPath.getFileName());
    try (var ignored = Files.newInputStream(cvPath)) {
      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("purpose", "user_data");
      body.add("file", new FileSystemResource(cvPath));

      UploadFileResponse uploadResponse = restClient
          .post()
          .uri(URI.create(API_URI))
          .headers(h -> h.setBearerAuth(properties.getGpt().getApiKey()))
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(body)
          .retrieve()
          .onStatus(HttpStatusCode::isError, (req, res) -> {
            log.error("ChatGPT job API returned {} - {}", res.getStatusCode().value(), res.getBody());
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Fail to upload file to GPT error code " + res.getStatusCode() + ", " + res.getStatusText());
          })
          .body(UploadFileResponse.class);

      if (uploadResponse == null) {
        return null;
      }
      return new ResumeFileInfo(uploadResponse.id(), uploadResponse.filename(), uploadResponse.expires_at());
    }
  }

  @Override
  @Bulkhead(name = "gptBulkhead")
  @RateLimiter(name = "gptLimiter")
  public void deleteFile(@NotBlank String fileId) {
    String bodyResponse = restClient
        .delete()
        .uri(URI.create(API_URI + "/" + fileId))
        .headers(h -> h.setBearerAuth(properties.getGpt().getApiKey()))
        .retrieve()
        .body(String.class);
    log.info("gpt Deleted fileId:{} with bodyResponse: {}", fileId, bodyResponse);
  }

  @Override
  public void deleteAllFilesExcept(@NotBlank List<String> fileIds) {
    FileListResponse response = restClient.get()
        .uri(API_URI)
        .headers((h) -> h.setBearerAuth(properties.getGpt().getApiKey()))
        .retrieve()
        .onStatus(HttpStatusCode::isError, (req, res) -> {
          log.error("GPT job API returned {} - {}", res.getStatusCode().value(), res.getBody());
        })
        .body(FileListResponse.class);

    if (response != null) {
      List<FileInfo> toDelete = response.data().stream().filter(f -> !fileIds.contains(f.id())).toList();
      for (FileInfo file : toDelete) {
        deleteFile(file.id());
      }
      log.info("Deleted {} files from GPT", toDelete.size());
    }
  }

  @SuppressWarnings("unused")
  private ResumeFileInfo fallbackUploadFile(Path cvPath, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return null;
  }

}
