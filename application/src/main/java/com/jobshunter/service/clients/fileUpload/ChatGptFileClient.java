package com.jobshunter.service.clients.fileUpload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.config.PackageExpected;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public final class ChatGptFileClient implements GPTFilesApiClient {

  private static final String API_URI = "https://api.openai.com/v1/files";

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private ApplicationProperties properties;

  @Autowired
  private JsonMapper mapper;

  @Override
  public String uploadFile(Path cvPath) throws IOException {
    ApplicationProperties.ChatGpt cfg = properties.getChatgpt();
    if (cfg == null || cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
      log.warn("ChatGPT upload requested but configuration or apiKey missing.");
      return null;
    }
    return uploadFile(cfg, cvPath);
  }

  @Override
  public boolean deleteFile(String fileId) {
    if (fileId == null || fileId.isBlank()) {
      return false;
    }
    ApplicationProperties.ChatGpt cfg = properties.getChatgpt();
    if (cfg == null || cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
      log.warn("ChatGPT delete requested but configuration or apiKey missing.");
      return false;
    }
    try {
      return deleteFile(cfg, fileId) != null;
    } catch (Exception e) {
      log.warn("Failed to delete ChatGPT file {}: {}", fileId, e.getMessage());
      return false;
    }
  }

  private String uploadFile(ApplicationProperties.ChatGpt cfg, Path cvPath) throws IOException {
    try (var ignored = Files.newInputStream(cvPath)) {
      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Bearer " + cfg.getApiKey());
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

      ChatGptFileClient.UploadFileResponse responseMapper = mapper.readValue(response.getBody(), ChatGptFileClient.UploadFileResponse.class);
      return responseMapper.id();
    }
  }

  public String deleteFile(ApplicationProperties.ChatGpt cfg, String fileId) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + cfg.getApiKey());

    HttpEntity<Void> entity = new HttpEntity<>(headers);
    ResponseEntity<String> httpResponse = restTemplate.exchange(
        URI.create(API_URI + "/" + fileId),
        HttpMethod.DELETE,
        entity,
        String.class
    );

    if (httpResponse.getStatusCode().value() >= 400) {
      log.warn("ChatGPT job API returned {} - {}", httpResponse.getStatusCode(), httpResponse.getBody());
      return null;
    }
    ChatGptFileClient.DeleteFileResponse respoanse = mapper.readValue(httpResponse.getBody(), ChatGptFileClient.DeleteFileResponse.class);
    return respoanse.id();
  }


  private sealed interface InputObj permits ChatGptFileClient.InputMessage, ChatGptFileClient.InputFile {

  }

  private record InputMessage(String type, String text) implements ChatGptFileClient.InputObj {

  }

  private record InputFile(String type, String file_id) implements ChatGptFileClient.InputObj {

  }


  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ChatCompletionResponse(List<ChatGptFileClient.OutputItem> output) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record UploadFileResponse(String id) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record DeleteFileResponse(String id) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record OutputItem(String id, String type, String status, List<ChatGptFileClient.ContentItem> content) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ContentItem(String type, String text) {

  }
}
