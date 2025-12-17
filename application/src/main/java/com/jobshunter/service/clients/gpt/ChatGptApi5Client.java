package com.jobshunter.service.clients.gpt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.config.ApplicationProperties.ChatGpt5;
import com.jobshunter.dto.Job;
import com.jobshunter.processor.PackageExpected;
import io.jsonwebtoken.lang.Collections;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@PackageExpected("com.jobshunter.service.application")
public non-sealed class ChatGptApi5Client extends AbstractGptApiClient<ChatGpt5> {

  private static final URI DEFAULT_URI = URI.create("https://api.openai.com/v1/responses");

  @Autowired
  private RestClient restClient;

  @Autowired
  private ApplicationProperties properties;

  @Autowired
  private JsonMapper mapper;

  @Override
  public ChatGpt5 getConfig() {
    return properties.getChatgpt5();
  }

  @Override
  public List<Job> searchWithModel(String systemPrompt, String userPrompt, ChatGpt5 cfg, String fileId) {
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

      ChatCompletionResponse response = restClient.post()
          .uri(DEFAULT_URI)
          .header("Authorization", "Bearer " + cfg.getApiKey())
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(ChatCompletionResponse.class);

      return extractJobs(response);
    } catch (Exception e) {
      log.error("ChatGPT job API call failed: {}", e.getMessage());
      return List.of();
    }
  }

  private List<Job> extractJobs(ChatCompletionResponse response) {
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
      return Arrays.stream(parsed).map(job -> new Job(job.score(), job.url(), "GPT")).toList();
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
  private record OutputItem(String id, String type, String status, List<ContentItem> content) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ContentItem(String type, String text) {

  }

}
