package com.jobshunter.service.clients.gpt;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.dto.gptRequest.GptJobsPayload;
import com.jobshunter.dto.gptRequest.Reasoning;
import com.jobshunter.dto.gptRequest.tools.Tools;
import com.jobshunter.dto.gptResponse.GptCompletionResponse;
import com.jobshunter.model.GptJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.AiJobsClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("PremiumJobsClientGPT")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "true")
public non-sealed class PremiumGptJobSearchImpl extends AbstractGptApiClient
    implements AiJobsClient<GptJobSearchRequest, List<Job>> {

  private static final URI DEFAULT_URI = URI.create("https://api.openai.com/v1/responses");

  private final RestClient restClient;

  private final ApplicationProperties properties;

  public PremiumGptJobSearchImpl(
      RestClient restClient,
      ApplicationProperties properties,
      com.jobshunter.service.clients.UrlExtractor urlExtractor
  ) {
    super(properties, urlExtractor);
    this.restClient = restClient;
    this.properties = properties;
  }

  @Override
  @CircuitBreaker(name = "gptCircuitBreaker", fallbackMethod = "fallbackSearch")
  @Bulkhead(name = "gptBulkhead", type = Bulkhead.Type.SEMAPHORE)
  @RateLimiter(name = "gptLimiter")
  public List<Job> searchWithModel(String systemPrompt, GptJobSearchRequest request) {
    try {
      GptJobsPayload payload = GptJobsPayload.builder()
          .model(request.getPrompt().getEngineConfiguration().getModel())
          .reasoning(new Reasoning("high"))
          .max_output_tokens(properties.getGpt().getMaxTokens())
          .addTools(Tools.builder().setDeepSearch().build())
          .addSystemPrompt(systemPrompt)
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
      log.error("ChatGPT job API call failed: {}", e.getMessage());
      return List.of();
    }
  }

  @SuppressWarnings("unused")
  private List<Job> fallbackSearch(String systemPrompt, GptJobSearchRequest request, Throwable t) {
    log.error("Premium GPT call short-circuited/bulkheaded: {}", t.getMessage());
    return List.of();
  }

  @Override
  public String getSystemPromptFilename() {
    return "jobsSystemPromptPremium.txt";
  }
}
