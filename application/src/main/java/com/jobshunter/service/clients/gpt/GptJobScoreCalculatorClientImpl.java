package com.jobshunter.service.clients.gpt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.ApplicationProperties;
import com.jobshunter.ApplicationProperties.Gpt;
import com.jobshunter.dto.gptRequest.Gpt4ScorePayload;
import com.jobshunter.dto.gptResponse.ContentItem;
import com.jobshunter.dto.gptResponse.GptResponse;
import com.jobshunter.dto.gptResponse.OutputItem;
import com.jobshunter.model.GptJobScoreRequest;
import com.jobshunter.model.PromptType;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.clients.JobScoreCalculatorClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.jsonwebtoken.lang.Collections;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
//TODO
//to collapse with GptV1JobSearchImpl
@Slf4j
@Component("GPTScoreCalculator")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "true")
@RequiredArgsConstructor
public non-sealed class GptJobScoreCalculatorClientImpl implements JobScoreCalculatorClient<GptJobScoreRequest> {

  private static final String AI_MODEL = "gpt-4o-mini";

  private static final URI DEFAULT_URI = URI.create("https://api.openai.com/v1/responses");

  private final ApplicationProperties properties;

  private final RestClient restClient;

  private final JsonMapper mapper;

  private final TemplateRenderer templateRenderer;

  @Override
  @RateLimiter(name = "gptLimiter")
  @CircuitBreaker(name = "gptCircuitBreaker", fallbackMethod = "fallbackComputeScore")
  @Bulkhead(name = "gptBulkhead")
  public int computeScore(GptJobScoreRequest request) {
    try {
      Gpt config = properties.getGpt();
      String userPrompt = templateRenderer.getPrompt(PromptType.USER_PROMPT_MATCH_SCORE, "description", request.getJobDescription());
      Gpt4ScorePayload payload = Gpt4ScorePayload.builder()
          .model(AI_MODEL)
          .temperature(0)
          .max_output_tokens(config.getMaxTokens())
          .addSystemPrompt(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_MATCH_SCORE))
          .addUserPrompt(userPrompt, request.getUserCV().getGptFileId())
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
    GptResponse response = mapper.readValue(body, GptResponse.class);
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

  @SuppressWarnings("unused")
  private int fallbackComputeScore(GptJobScoreRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return -1;
  }

}
