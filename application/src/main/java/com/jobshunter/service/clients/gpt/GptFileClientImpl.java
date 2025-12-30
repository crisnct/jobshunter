package com.jobshunter.service.clients.gpt;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.ApplicationProperties;
import com.jobshunter.dto.gptResponse.FileInfo;
import com.jobshunter.dto.gptResponse.FileListResponse;
import com.jobshunter.dto.gptResponse.UploadFileResponse;
import com.jobshunter.model.GptJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.FileClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component("Gpt")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "true")
@RequiredArgsConstructor
public non-sealed class GptFileClientImpl implements FileClient {

  private static final String API_URI = "https://api.openai.com/v1/files";

  private final RestTemplate restTemplate;
  private final RestClient restClient;
  private final ApplicationProperties properties;
  private final JsonMapper mapper;

  @Override
  @CircuitBreaker(name = "gptCircuitBreaker", fallbackMethod = "fallbackUploadFile")
  @Bulkhead(name = "gptBulkhead")
  public String uploadFile(Path cvPath) throws IOException {
    try (var ignored = Files.newInputStream(cvPath)) {
      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Bearer " + properties.getGpt().getApiKey());
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);

      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("purpose", "assistants");
      body.add("file", new FileSystemResource(cvPath));

      ResponseEntity<String> response
          = restTemplate.postForEntity(URI.create(API_URI), new HttpEntity<>(body, headers), String.class);
      if (response.getStatusCode().isError()) {
        log.warn("ChatGPT job API returned {} - {}", response.getStatusCode().value(), response.getBody());
        return null;
      }

      UploadFileResponse responseMapper = mapper.readValue(response.getBody(), UploadFileResponse.class);
      return responseMapper.id();
    }
  }

  @Override
  public void deleteFile(@NotBlank String fileId) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + properties.getGpt().getApiKey());
    restTemplate.exchange(
        URI.create(API_URI + "/" + fileId),
        HttpMethod.DELETE,
        new HttpEntity<>(headers),
        String.class
    );
  }

  @Override
  public void deleteAllFilesExcept(@NotBlank List<String> fileIds) {
    FileListResponse response = restClient.get()
        .uri(API_URI)
        .header("Authorization", "Bearer " + properties.getGpt().getApiKey())
        .retrieve()
        .body(FileListResponse.class);
    if (response != null && response.data() != null) {
      List<FileInfo> toDelete = response.data().stream().filter(f -> !fileIds.contains(f.id())).toList();
      for (FileInfo file : toDelete) {
        deleteFile(file.id());
      }
      log.info("Deleted {} files", toDelete.size());
    }
  }

  @SuppressWarnings("unused")
  private String fallbackUploadFile(Path cvPath, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return "";
  }

}
