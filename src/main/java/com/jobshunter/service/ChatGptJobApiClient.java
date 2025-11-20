package com.jobshunter.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.config.ApplicationProperties;
import io.jsonwebtoken.lang.Collections;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
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
public class ChatGptJobApiClient {

  private static final URI DEFAULT_URI = URI.create("https://api.openai.com/v1/responses");

  private static final String DELETE_URI = "https://api.openai.com/v1/files";

  private static final URI UPLOAD_URI = URI.create("https://api.openai.com/v1/files");

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private ApplicationProperties properties;

  private JsonMapper mapper;

  //key=file hash, value=file id
  private final Map<String, String> fileIds = new ConcurrentHashMap<>();

  @PostConstruct
  public void postInit() {
    mapper = JsonMapper.builder().findAndAddModules().build();
  }

  public List<String> search(String prompt, Path cvPath) throws IOException {
    ApplicationProperties.ChatGpt cfg = properties.getChatgpt();
    if (cfg == null) {
      return List.of();
    }
    if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
      log.warn("ChatGPT job search enabled but CHATGPT5_API_KEY missing.");
      return List.of();
    }

    String fileId = uploadFile(cfg, cvPath);
    return searchWithModel(prompt, cfg, fileId);
  }

  private String uploadFile(ApplicationProperties.ChatGpt cfg, Path cvPath) throws IOException {
    try (var in = Files.newInputStream(cvPath)) {
      String sha256 = DigestUtils.sha256Hex(in);

      String existingFileId = fileIds.get(sha256);
      if (existingFileId != null) {
        return existingFileId;
      }

      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Bearer " + cfg.getApiKey());
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);

      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("purpose", "assistants");
      body.add("file", new FileSystemResource(cvPath));

      ResponseEntity<String> response
          = restTemplate.postForEntity(UPLOAD_URI, new HttpEntity<>(body, headers), String.class);
      if (response.getStatusCode().isError()) {
        log.warn("ChatGPT job API returned {} - {}", response.getStatusCode().value(), response.getBody());
        return null;
      }

      UploadFileResponse responseMapper = mapper.readValue(response.getBody(), UploadFileResponse.class);
      fileIds.put(sha256, responseMapper.id());
      return responseMapper.id();
    }
  }

  private String deleteFile(ApplicationProperties.ChatGpt cfg, String fileId) throws IOException, InterruptedException, URISyntaxException {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + cfg.getApiKey());

    HttpEntity<Void> entity = new HttpEntity<>(headers);
    ResponseEntity<String> httpResponse = restTemplate.exchange(
        URI.create(DELETE_URI + "/" + fileId),
        HttpMethod.DELETE,
        entity,
        String.class
    );

    if (httpResponse.getStatusCode().value() >= 400) {
      log.warn("ChatGPT job API returned {} - {}", httpResponse.getStatusCode(), httpResponse.getBody());
      return null;
    }
    DeleteFileResponse respoanse = mapper.readValue(httpResponse.getBody(), DeleteFileResponse.class);
    return respoanse.id();
  }

  private List<String> searchWithModel(String prompt, ApplicationProperties.ChatGpt cfg, String fileId) {
    try {
      ChatGptPayload payload = new ChatGptPayload(
          cfg.getModel(),
          cfg.getTemperature(),
          cfg.getMaxTokens(),
          List.of(new Tools(cfg.getToolsType())),
          List.of(
              new Input("system", List.of(new InputMessage("input_text", cfg.getSystemPrompt()))),
              new Input("user", List.of(
                  new InputMessage("input_text", prompt),
                  new InputFile(fileId)
              ))
          )
      );

      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Bearer " + cfg.getApiKey());
      headers.setContentType(MediaType.APPLICATION_JSON);

      String jsonBody = mapper.writeValueAsString(payload);
      HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

      ResponseEntity<String> response = restTemplate.postForEntity(
          DEFAULT_URI,
          entity,
          String.class
      );

      if (response.getStatusCode().value() >= 400) {
        log.warn("ChatGPT job API returned {} - {}", response.getStatusCode(), response.getBody());
        return List.of();
      }
      return extractJobs(response.getBody(), cfg.getMaxJobs());
    } catch (Exception e) {
      log.warn("ChatGPT job API call failed: {}", e.getMessage());
      return List.of();
    }
  }

  private List<String> extractJobs(String body, int maxJobs) throws JsonProcessingException {
    ChatCompletionResponse response = mapper.readValue(body, ChatCompletionResponse.class);
    if (Collections.isEmpty(response.output())) {
      return List.of();
    }
    Optional<OutputItem> item = response.output().stream()
        .filter(p -> Objects.equals(p.type, "message") && !p.content().isEmpty())
        .findAny();
    if (item.isPresent()) {
      final List<String> jobs = new ArrayList<>();
      item.get().content.stream()
          .filter(c -> Objects.equals("output_text", c.type))
          .forEach(o -> jobs.addAll(List.of(o.text.split(" "))));
      return jobs;
    } else {
      return java.util.Collections.emptyList();
    }
  }

  private record ChatGptPayload(
      String model,
      double temperature,
      int max_output_tokens,
      List<Tools> tools,
      List<Input> input
  ) {

  }

  private interface InputObj {

  }

  private record Input(String role, List<InputObj> content) {

  }

  private record InputMessage(String type, String text) implements InputObj {

  }

  private record InputFile(String type, String file_id) implements InputObj {

    public InputFile(String file_id) {
      this("input_file", file_id);
    }
  }

  private record Tools(String type) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ChatCompletionResponse(List<OutputItem> output) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record UploadFileResponse(String id) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record DeleteFileResponse(String id) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record OutputItem(String id, String type, String status, List<ContentItem> content) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ContentItem(String type, String text) {

  }

  @PreDestroy
  public void beforeShutdown() {
    for (String fileId : fileIds.values()) {
      try {
        String fileIdDeleted = deleteFile(properties.getChatgpt(), fileId);
        log.info("File {} deleted from chatgpt", fileIdDeleted);
      } catch (InterruptedException | URISyntaxException | IOException e) {
        log.error("File {} can not be deleted from chatgpt", fileId);
      }
    }
  }

}
