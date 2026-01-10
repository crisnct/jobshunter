package com.jobshunter.service.clients.gpt;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.ApplicationProperties.Gpt;
import com.jobshunter.database.entities.UserRemoteCvEntity;
import com.jobshunter.dto.exceptions.ValidationException;
import com.jobshunter.dto.gptRequest.Gpt4ScorePayload;
import com.jobshunter.dto.gptResponse.ContentItem;
import com.jobshunter.dto.gptResponse.GptResponse;
import com.jobshunter.dto.gptResponse.OutputItem;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.GptJobScoreRequest;
import com.jobshunter.model.PromptType;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.security.JHHeaders;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.clients.JobScoreCalculatorClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("GptJobScoreCalculator")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "true")
@RequiredArgsConstructor
public non-sealed class GptJobScoreCalculatorClientImpl implements JobScoreCalculatorClient<GptJobScoreRequest> {

  private static final String AI_MODEL = "gpt-5.2";

  private static final URI DEFAULT_URI = URI.create("https://api.openai.com/v1/responses");

  private final ApplicationProperties properties;

  private final RestClient restClient;

  private final TemplateRenderer templateRenderer;

  @Override
  @RateLimiter(name = "gptLimiter")
  @CircuitBreaker(name = "gptCircuitBreaker", fallbackMethod = "fallbackComputeScore")
  @Bulkhead(name = "gptBulkhead")
  public int computeScore(GptJobScoreRequest request) {
    try {
      String userPrompt = templateRenderer.getPrompt(PromptType.USER_PROMPT_MATCH_SCORE, "description", request.getJobDescription());
      UserRemoteCvEntity remoteCV = request.getUserCV().getUser().getRemoteCvs().stream()
          .filter(p -> p.getProvider() == EngineType.GPT).findAny()
          .orElseThrow(() -> new ValidationException("No GPT CV found for user " + request.getUserCV().getUser().getUsername()));

      Gpt4ScorePayload payload = Gpt4ScorePayload.builder()
          .model(AI_MODEL)
          .temperature(0)
          .max_output_tokens(16)
          .reasoning(request.getReasoning())
          .store(false)
          .addSystemPrompt(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_MATCH_SCORE))
          .addUserPrompt(userPrompt, remoteCV.getFileId())
          .build();

      GptResponse response = restClient.post()
          .uri(DEFAULT_URI)
          .headers((h) -> h.setBearerAuth(properties.getGpt().getApiKey()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(GptResponse.class);

      //noinspection DataFlowIssue
      return extractScore(response);
    } catch (Exception e) {
      log.error("❌ GPT job API call failed", e);
      return 0;
    }
  }

  private int extractScore(GptResponse response) {
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
