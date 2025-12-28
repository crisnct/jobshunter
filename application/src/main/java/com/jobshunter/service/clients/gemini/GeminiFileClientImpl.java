package com.jobshunter.service.clients.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jobshunter.ApplicationProperties;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.FileClient;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component("Gemini")
@PackageExpected("com.jobshunter.service.clients.gemini")
@ConditionalOnProperty(name = "gemini.enabled", havingValue = "true")
@RequiredArgsConstructor
public final class GeminiFileClientImpl implements FileClient {

  public static final String UPLOAD_URI = "https://generativelanguage.googleapis.com/upload/v1beta/files";

  private static final String DELETE_URI = "https://generativelanguage.googleapis.com/v1beta";

  private final RestClient restClient;

  private final ApplicationProperties properties;

  @Override
  public String uploadFile(Path cvPath) throws IOException {
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
      return response.file().name();
    }
  }

  @Override
  public void deleteFile(@NotBlank String fileId) {
    restClient.delete()
        .uri(DELETE_URI + "/" + fileId + "?key=" + properties.getGemini().getApiKey())
        //.attribute("key", properties.getGemini().getApiKey())
        .retrieve()
        .body(Void.class);
  }

  @Override
  public void deleteAllFilesExcept(List<String> fileIds) {
    throw new RuntimeException("not implemented");
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record UploadFileResponse(FileResponse file) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FileResponse(String name) {

  }

}
