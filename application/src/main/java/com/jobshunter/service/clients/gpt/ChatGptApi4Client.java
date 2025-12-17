package com.jobshunter.service.clients.gpt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.config.ApplicationProperties.ChatGpt4;
import com.jobshunter.dto.Job;
import com.jobshunter.processor.PackageExpected;
import io.jsonwebtoken.lang.Collections;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@PackageExpected("com.jobshunter.service.application")
public non-sealed class ChatGptApi4Client extends AbstractGptApiClient<ChatGpt4> {

  private static final URI DEFAULT_URI = URI.create("https://api.openai.com/v1/responses");

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private ApplicationProperties properties;

  private String calculateScoreUserPrompt;

  @Autowired
  private JsonMapper mapper;

  @Override
  public ChatGpt4 getConfig() {
    return properties.getChatgpt4();
  }

  @SuppressWarnings("DataFlowIssue")
  @PostConstruct
  public void init(){
    super.init();
      try (var inputStream = getClass().getClassLoader().getResourceAsStream(
          "prompts/calculateScoreUserPrompt.txt")) {
        calculateScoreUserPrompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      } catch (Exception e) {
        throw new IllegalStateException("Cannot load system prompt file", e);
      }
  }

  @Override
  public List<Job> searchWithModel(String systemPrompt, String userPrompt, ChatGpt4 cfg, String fileId) {
    try {
      Gpt4Payload payload = new Gpt4Payload(
          cfg.getModel(),
          cfg.getTemperature(),
          cfg.getMaxTokens(),
          List.of(
              new ChatGptApi4Client.Input("system", List.of(new ChatGptApi4Client.InputMessage("input_text", systemPrompt))),
              new ChatGptApi4Client.Input("user", List.of(
                  new ChatGptApi4Client.InputMessage("input_text", userPrompt),
                  new ChatGptApi4Client.InputFile(fileId)
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
    ChatGptApi4Client.ChatCompletionResponse response = mapper.readValue(body, ChatGptApi4Client.ChatCompletionResponse.class);
    if (Collections.isEmpty(response.output())) {
      return List.of();
    }
    Optional<ChatGptApi4Client.OutputItem> item = response.output().stream()
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

  private record Gpt4Payload(
      String model,
      double temperature,
      int max_output_tokens,
      List<ChatGptApi4Client.Input> input
  ) {

  }

  private sealed interface InputObj permits ChatGptApi4Client.InputMessage, ChatGptApi4Client.InputFile {

  }

  private record Input(String role, List<ChatGptApi4Client.InputObj> content) {

  }

  private record InputMessage(String type, String text) implements ChatGptApi4Client.InputObj {

  }

  private record InputFile(String type, String file_id) implements ChatGptApi4Client.InputObj {

    public InputFile(String file_id) {
      this("input_file", file_id);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ChatCompletionResponse(List<ChatGptApi4Client.OutputItem> output) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record OutputItem(String id, String type, String status, List<ChatGptApi4Client.ContentItem> content) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ContentItem(String type, String text) {

  }

  public final int computeScore(String jobDescription, String fileId) {
    try {
      Gpt4Payload payload = new Gpt4Payload(
          getConfig().getModel(),
          getConfig().getTemperature(),
          getConfig().getMaxTokens(),
          List.of(
              new ChatGptApi4Client.Input("system",
                  List.of(new ChatGptApi4Client.InputMessage("input_text", getSystemPrompt()))),
              new ChatGptApi4Client.Input("user", List.of(
                  new ChatGptApi4Client.InputMessage("input_text", calculateScoreUserPrompt + jobDescription),
                  new ChatGptApi4Client.InputFile(fileId)
              ))
          )
      );

      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Bearer " + getConfig().getApiKey());
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
        return 0;
      }
      return extractScore(response.getBody());
    } catch (Exception e) {
      log.warn("ChatGPT job API call failed: {}", e.getMessage());
      return 0;
    }
  }

  private int extractScore(String body) throws JsonProcessingException {
    ChatGptApi4Client.ChatCompletionResponse response = mapper.readValue(body, ChatGptApi4Client.ChatCompletionResponse.class);
    if (Collections.isEmpty(response.output())) {
      return 0;
    }
    Optional<ChatGptApi4Client.OutputItem> item = response.output().stream()
        .filter(p -> Objects.equals(p.type, "message") && !p.content().isEmpty())
        .findAny();
    if (item.isPresent()) {
      String score = item.get().content.stream()
          .filter(c -> Objects.equals("output_text", c.type))
          .findFirst()
          .orElseGet(()-> new ContentItem("output_text", "0"))
          .text;
      return Integer.parseInt(score);
    } else {
      return 0;
    }
  }

}
