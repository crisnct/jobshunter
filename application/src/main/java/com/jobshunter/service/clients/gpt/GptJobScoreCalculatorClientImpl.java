package com.jobshunter.service.clients.gpt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.ApplicationProperties;
import com.jobshunter.ApplicationProperties.Gpt;
import com.jobshunter.dto.gptRequest.Gpt4ScorePayload;
import com.jobshunter.dto.gptResponse.ContentItem;
import com.jobshunter.dto.gptResponse.GptCompletionResponse;
import com.jobshunter.dto.gptResponse.OutputItem;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.GptJobScoreCalculatorClient;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.jsonwebtoken.lang.Collections;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "true")
public non-sealed class GptJobScoreCalculatorClientImpl implements GptJobScoreCalculatorClient {

  private static final URI DEFAULT_URI = URI.create("https://api.openai.com/v1/responses");

  @Autowired
  private ApplicationProperties properties;

  @Autowired
  private RestClient restClient;

  private String calculateScoreSystemPrompt;

  private String calculateScoreUserPrompt;

  @Autowired
  private JsonMapper mapper;

  @PostConstruct
  @SuppressWarnings("DataFlowIssue")
  public void init() throws IOException {
    try (var inputStream = getClass().getClassLoader().getResourceAsStream(
        "prompts/gptScoreUserPrompt.txt")) {
      calculateScoreUserPrompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
    try (var inputStream = getClass().getClassLoader().getResourceAsStream(
        "prompts/gptScoreSystemPrompt.txt")) {
      calculateScoreSystemPrompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Override
  @RateLimiter(name = "openaiLimiter")
  public int computeScore(String jobDescription, String fileId) {
    try {
      Gpt config = properties.getGpt();
      Gpt4ScorePayload payload = Gpt4ScorePayload.builder()
          .model(config.getEconomy().getModel())
          .temperature(0)
          .max_output_tokens(config.getMaxTokens())
          .addSystemPrompt(calculateScoreSystemPrompt)
          .addUserPrompt(calculateScoreUserPrompt + jobDescription, fileId)
          .build();

      String response = restClient.post()
          .uri(DEFAULT_URI)
          .header("Authorization", "Bearer " + config.getApiKey())
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
    GptCompletionResponse response = mapper.readValue(body, GptCompletionResponse.class);
    if (Collections.isEmpty(response.output())) {
      return 0;
    }
    Optional<OutputItem> item = response.output().stream()
        .filter(p -> Objects.equals(p.type(), "message") && !p.content().isEmpty())
        .findAny();
    if (item.isPresent()) {
      String score = item.get().content().stream()
          .filter(c -> Objects.equals("output_text", c.type()))
          .findFirst()
          .orElseGet(() -> new ContentItem("output_text", "0"))
          .text();
      return Integer.parseInt(score);
    } else {
      return 0;
    }
  }

}
