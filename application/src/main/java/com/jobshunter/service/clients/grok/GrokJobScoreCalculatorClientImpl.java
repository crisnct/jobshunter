package com.jobshunter.service.clients.grok;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.entities.UserRemoteCvEntity;
import com.jobshunter.dto.exceptions.ValidationException;
import com.jobshunter.dto.grokRequest.GrokScorePayload;
import com.jobshunter.dto.grokRequest.Reasoning;
import com.jobshunter.dto.grokResponse.ContentItem;
import com.jobshunter.dto.grokResponse.GrokResponse;
import com.jobshunter.dto.grokResponse.OutputItem;
import com.jobshunter.dto.grokResponse.Usage;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.JobScoreRequest;
import com.jobshunter.model.PromptType;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.cost.AiRequestCostEvent;
import com.jobshunter.service.application.cost.TokensConsumedMapper;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("GrokJobScoreCalculator")
@PackageExpected("com.jobshunter.service.clients.grok")
@ConditionalOnProperty(name = "grok.enabled", havingValue = "true")
@RequiredArgsConstructor
public non-sealed class GrokJobScoreCalculatorClientImpl implements JobScoreCalculatorClient {

  private static final URI DEFAULT_URI = URI.create("https://api.x.ai/v1/responses");

  private final ApplicationProperties properties;

  private final RestClient restClient;

  private final TemplateRenderer templateRenderer;

  private final ApplicationEventPublisher eventPublisher;

  @Override
  @RateLimiter(name = "grokLimiter")
  @CircuitBreaker(name = "grokCircuitBreaker", fallbackMethod = "fallbackComputeScore")
  @Bulkhead(name = "grokBulkhead")
  public int computeScore(JobScoreRequest request) {
    try {
      UserRemoteCvEntity remoteCV = request.getUserCV().getUser().getRemoteCvs().stream()
          .filter(p -> p.getProvider() == EngineType.GROK).findAny()
          .orElseThrow(() -> new ValidationException("No GROK CV found for user" + request.getUserCV().getUser().getUsername()));

      String userPrompt = templateRenderer.getPrompt(PromptType.USER_PROMPT_MATCH_SCORE, "description", request.getJobDescription());
      GrokScorePayload payload = GrokScorePayload.builder(request.getModel())
          .store(false)
          .reasoning(new Reasoning(REASONING_SCORING))
          .max_output_tokens(16)
          .addSystemPrompt(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_MATCH_SCORE))
          .addUserPrompt(userPrompt, remoteCV.getFileId())
          .build();

      GrokResponse response = restClient.post()
          .uri(DEFAULT_URI)
          .headers((h) -> h.setBearerAuth(properties.getGrok().getApiKey()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(GrokResponse.class);

      Usage usage = response.usage();
      if (usage != null) {
        eventPublisher.publishEvent(new AiRequestCostEvent(
            this,
            request.getOrder() != null ? request.getOrder().getJobOrder().getId(): -1,
            payload.aiModel(),
            TokensConsumedMapper.fromGrok(usage))
        );
      }

      //noinspection DataFlowIssue
      return extractScore(response);
    } catch (Exception e) {
      log.error("GROK job API call failed", e);
      return 0;
    }
  }

  private int extractScore(GrokResponse response) {
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
  private int fallbackComputeScore(JobScoreRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return -1;
  }

}
