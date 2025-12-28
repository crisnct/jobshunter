package com.jobshunter.service.clients.gpt;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.dto.gptRequest.GptJobsPayload;
import com.jobshunter.dto.gptRequest.tools.Tools;
import com.jobshunter.dto.gptResponse.GptCompletionResponse;
import com.jobshunter.model.GptJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.application.UrlExtractor;
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
@Component("EconomyJobsClientGPT")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "true")
public non-sealed class EconomyGptJobSearchImpl extends AbstractGptApiClient
    implements AiJobsClient<GptJobSearchRequest, List<Job>> {

  private static final URI DEFAULT_URI = URI.create("https://api.openai.com/v1/responses");

  private final ApplicationProperties properties;

  private final RestClient restClient;

  public EconomyGptJobSearchImpl(
      ApplicationProperties properties,
      RestClient restClient,
      UrlExtractor urlExtractor
  ) {
    super(properties, urlExtractor);
    this.properties = properties;
    this.restClient = restClient;
  }

  @Override
  @CircuitBreaker(name = "gptCircuitBreaker", fallbackMethod = "fallbackSearch")
  @Bulkhead(name = "gptBulkhead", type = Bulkhead.Type.SEMAPHORE)
  @RateLimiter(name = "gptLimiter")
  public List<Job> searchJobs(GptJobSearchRequest request) {
    try {
      GptJobsPayload payload = GptJobsPayload.builder()
          .model(request.getPrompt().getEngineConfiguration().getModel())
          .max_output_tokens(properties.getGpt().getMaxTokens())
          .addTools(Tools.builder().setDeepSearch().build())
          .addSystemPrompt(getJobsSystemPrompt())
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

  @Override
  public String getSystemPromptFilename() {
    return "jobsSystemPromptEconomy.txt";
  }

  @SuppressWarnings("unused")
  private List<Job> fallbackSearch(GptJobSearchRequest request, Throwable t) {
    log.error("Economy GPT call short-circuited/bulkheaded: {}", t.getMessage());
    return List.of();
  }

}
