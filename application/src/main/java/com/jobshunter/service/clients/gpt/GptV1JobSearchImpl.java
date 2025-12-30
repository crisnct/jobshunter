package com.jobshunter.service.clients.gpt;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.dto.gptRequest.GptJobsPayload;
import com.jobshunter.dto.gptRequest.Reasoning;
import com.jobshunter.dto.gptRequest.tools.Tools;
import com.jobshunter.dto.gptResponse.GptCompletionResponse;
import com.jobshunter.dto.gptResponse.OutputItem;
import com.jobshunter.model.GptJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.AiMessage;
import com.jobshunter.service.AiMessage.AiMessageType;
import com.jobshunter.service.application.UrlExtractor;
import com.jobshunter.service.clients.AiJobsClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.jsonwebtoken.lang.Collections;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("JobsClientGPT")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "true")
@AllArgsConstructor
public non-sealed class GptV1JobSearchImpl implements AiJobsClient<GptJobSearchRequest, List<Job>> {

  public static final URI DEFAULT_URI = URI.create("https://api.openai.com/v1/responses");

  private final ApplicationProperties properties;

  private final RestClient restClient;

  private final UrlExtractor urlExtractor;

  @Override
  @CircuitBreaker(name = "gptCircuitBreaker", fallbackMethod = "fallbackSearch")
  @RateLimiter(name = "gptLimiter")
  @Bulkhead(name = "gptBulkhead")
  public List<Job> searchJobs(GptJobSearchRequest request) {
    try {
      GptJobsPayload payload = GptJobsPayload.builder()
          .model(request.getPrompt().getEngineConfiguration().getModel())
          .reasoning(request.getReasoning())
          .max_output_tokens(properties.getGpt().getMaxTokens())
          .addTools(Tools.builder().setDeepSearch().build())
          .addSystemPrompt(AiMessage.of(AiMessageType.SYSTEM_PROMPT_JOB_SEARCH))
          .addUserPrompt(request.getPrompt().getPrompt(), request.getFileId())
          .build();

      GptCompletionResponse response = restClient.post()
          .uri(DEFAULT_URI)
          .header("Authorization", "Bearer " + properties.getGpt().getApiKey())
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

  @SuppressWarnings("unused")
  private List<Job> fallbackSearch(GptJobSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return List.of();
  }

  protected List<Job> extractJobs(GptCompletionResponse response) {
    if (Collections.isEmpty(response.output())) {
      return List.of();
    }
    Optional<OutputItem> item = response.output().stream()
        .filter(p -> Objects.equals(p.type(), "message") && !p.content().isEmpty())
        .findAny();
    if (item.isPresent()) {
      final List<Job> jobs = new ArrayList<>();
      item.get().content().stream()
          .filter(c -> c.text().length() > 2)
          .filter(c -> Objects.equals("output_text", c.type()))
          .forEach(o -> jobs.addAll(urlExtractor.parseJobs(o.text())));
      return jobs;
    } else {
      return java.util.Collections.emptyList();
    }
  }
}
