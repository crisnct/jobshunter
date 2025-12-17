package com.jobshunter.service.clients.gpt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.config.ApplicationProperties.Gpt4;
import com.jobshunter.dto.Job;
import com.jobshunter.processor.PackageExpected;
import io.jsonwebtoken.lang.Collections;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@PackageExpected("com.jobshunter.service.application")
public non-sealed class GptApi4Client extends AbstractGptApiClient<Gpt4> {

  private static final URI DEFAULT_URI = URI.create("https://api.openai.com/v1/responses");

  @Autowired
  private ApplicationProperties properties;

  @Autowired
  private RestClient restClient;

  private String calculateScoreSystemPrompt;

  private String calculateScoreUserPrompt;

  @Autowired
  private JsonMapper mapper;

  @Override
  public Gpt4 getConfig() {
    return properties.getGpt4();
  }

  @SuppressWarnings("DataFlowIssue")
  @PostConstruct
  public void init() throws IOException {
    super.init();
    try (var inputStream = getClass().getClassLoader().getResourceAsStream(
        "prompts/scoreUserPrompt.txt")) {
      calculateScoreUserPrompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
    try (var inputStream = getClass().getClassLoader().getResourceAsStream(
        "prompts/scoreSystemPrompt.txt")) {
      calculateScoreSystemPrompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Override
  public List<Job> searchWithModel(String systemPrompt, String userPrompt, Gpt4 cfg, String fileId) {
    try {
      Gpt4Payload payload = new Gpt4Payload(
          cfg.getModel(),
          cfg.getTemperature(),
          cfg.getMaxTokens(),
          List.of(
              new GptApi4Client.Input("system", List.of(new GptApi4Client.InputMessage("input_text", systemPrompt))),
              new GptApi4Client.Input("user", List.of(
                  new GptApi4Client.InputMessage("input_text", userPrompt),
                  new GptApi4Client.InputFile(fileId)
              ))
          )
      );

      GptCompletionResponse response = restClient.post()
          .uri(DEFAULT_URI)
          .header("Authorization", "Bearer " + cfg.getApiKey())
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(GptCompletionResponse.class);

      //noinspection DataFlowIssue
      return extractJobs(response);
    } catch (Exception e) {
      log.error("ChatGPT job API call failed", e);
      return List.of();
    }
  }

  private record Gpt4Payload(
      String model,
      double temperature,
      int max_output_tokens,
      List<GptApi4Client.Input> input
  ) {

  }

  private sealed interface InputObj permits GptApi4Client.InputMessage, GptApi4Client.InputFile {

  }

  private record Input(String role, List<GptApi4Client.InputObj> content) {

  }

  private record InputMessage(String type, String text) implements GptApi4Client.InputObj {

  }

  private record InputFile(String type, String file_id) implements GptApi4Client.InputObj {

    public InputFile(String file_id) {
      this("input_file", file_id);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ChatCompletionResponse(List<GptApi4Client.OutputItem> output) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record OutputItem(String id, String type, String status, List<GptApi4Client.ContentItem> content) {

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
              new GptApi4Client.Input("system",
                  List.of(new GptApi4Client.InputMessage("input_text", calculateScoreSystemPrompt))),
              new GptApi4Client.Input("user", List.of(
                  new GptApi4Client.InputMessage("input_text", calculateScoreUserPrompt + jobDescription),
                  new GptApi4Client.InputFile(fileId)
              ))
          )
      );

      String response = restClient.post()
          .uri(DEFAULT_URI)
          .header("Authorization", "Bearer " + getConfig().getApiKey())
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(String.class);

      return extractScore(response);
    } catch (Exception e) {
      log.error("ChatGPT job API call failed", e);
      return 0;
    }
  }

  private int extractScore(String body) throws JsonProcessingException {
    GptApi4Client.ChatCompletionResponse response = mapper.readValue(body, GptApi4Client.ChatCompletionResponse.class);
    if (Collections.isEmpty(response.output())) {
      return 0;
    }
    Optional<GptApi4Client.OutputItem> item = response.output().stream()
        .filter(p -> Objects.equals(p.type, "message") && !p.content().isEmpty())
        .findAny();
    if (item.isPresent()) {
      String score = item.get().content.stream()
          .filter(c -> Objects.equals("output_text", c.type))
          .findFirst()
          .orElseGet(() -> new ContentItem("output_text", "0"))
          .text;
      return Integer.parseInt(score);
    } else {
      return 0;
    }
  }

}
