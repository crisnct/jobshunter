package com.jobshunter.service.clients.jobSearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.dto.Job;
import com.jobshunter.processor.PackageExpected;
import io.jsonwebtoken.lang.Collections;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@ConditionalOnProperty(value = "jobshunter.chatGpt.model", havingValue = "gpt-5.1", matchIfMissing = false)
@PackageExpected("com.jobshunter.service.application")
public final class ChatGptApi5Client implements GPTSearchApiClient {

  private static final URI DEFAULT_URI = URI.create("https://api.openai.com/v1/responses");

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private ApplicationProperties properties;

  @Autowired
  private JsonMapper mapper;

  public List<Job> search(String systemPrompt, String userPrompt, String fileId) {
    ApplicationProperties.ChatGpt cfg = properties.getChatgpt();
    if (cfg == null) {
      return List.of();
    }
    if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
      log.warn("ChatGPT job search enabled but CHATGPT5_API_KEY missing.");
      return List.of();
    }

    return searchWithModel(systemPrompt, userPrompt, cfg, fileId);
  }

  private List<Job> searchWithModel(String systemPrompt, String userPrompt, ApplicationProperties.ChatGpt cfg, String fileId) {
    try {
      ChatGptPayload payload = new ChatGptPayload(
          cfg.getModel(),
          cfg.getTemperature(),
          cfg.getMaxTokens(),
          List.of(new Tools(cfg.getToolsType())),
          List.of(
              new Input("system", List.of(new InputMessage("input_text", systemPrompt))),
              new Input("user", List.of(
                  new InputMessage("input_text", userPrompt),
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
      return extractJobs(response.getBody());
    } catch (Exception e) {
      log.warn("ChatGPT job API call failed: {}", e.getMessage());
      return List.of();
    }
  }

  private List<Job> extractJobs(String body) throws JsonProcessingException {
    ChatCompletionResponse response = mapper.readValue(body, ChatCompletionResponse.class);
    if (Collections.isEmpty(response.output())) {
      return List.of();
    }
    Optional<OutputItem> item = response.output().stream()
        .filter(p -> Objects.equals(p.type, "message") && !p.content().isEmpty())
        .findAny();
    if (item.isPresent()) {
      final List<Job> jobs = new ArrayList<>();
      item.get().content.stream()
          .filter(c -> Objects.equals("output_text", c.type))
          .forEach(o -> jobs.addAll(parseJobs(o.text)));
      return jobs;
    } else {
      return java.util.Collections.emptyList();
    }
  }

  private List<Job> parseJobs(String text) {
    if (Strings.isBlank(text)) {
      return List.of();
    }
    try {
      Job[] parsed = mapper.readValue(text, Job[].class);
      return List.of(parsed);
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse jobs from ChatGPT response: {}", e.getMessage());
      return List.of();
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

  private sealed interface InputObj permits InputMessage, InputFile {

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

}
