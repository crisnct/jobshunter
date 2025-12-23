package com.jobshunter.service.clients.gpt;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.config.ApplicationProperties.ModelSpecific;
import com.jobshunter.dto.Job;
import com.jobshunter.dto.gptRequest.GptJobSearchRequest;
import com.jobshunter.dto.gptRequest.GptJobsPayload;
import com.jobshunter.dto.gptRequest.tools.Tools;
import com.jobshunter.dto.gptResponse.GptCompletionResponse;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.AiJobsClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("EconomyJobsClientGPT")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "jobshunter.useDummyData", havingValue = "false")
public non-sealed class EconomyGptJobSearchImpl extends AbstractGptApiClient
    implements AiJobsClient<GptJobSearchRequest, List<Job>> {

  private static final URI DEFAULT_URI = URI.create("https://api.openai.com/v1/responses");

  @Autowired
  private ApplicationProperties properties;

  @Autowired
  private RestClient restClient;

  @Override
  public ModelSpecific getConfig() {
    return properties.getGpt().getEconomy();
  }

  @Override
  @CircuitBreaker(name = "gptEconomy", fallbackMethod = "fallbackSearch")
  @Bulkhead(name = "gptEconomyBulkhead", type = Bulkhead.Type.SEMAPHORE)
  public List<Job> searchWithModel(String systemPrompt, String userPrompt, ModelSpecific cfg, String fileId) {
    try {
      GptJobsPayload payload = GptJobsPayload.builder()
          .model(cfg.getModel())
          .max_output_tokens(properties.getGpt().getMaxTokens())
          .addTools(Tools.builder().setDeepSearch().build())
          .setResponseSchema(getOutputSchema())
          .addSystemPrompt(systemPrompt)
          .addUserPrompt(userPrompt, fileId)
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
  private List<Job> fallbackSearch(String systemPrompt, String userPrompt, ModelSpecific cfg, String fileId, Throwable t) {
    log.error("Economy GPT call short-circuited/bulkheaded: {}", t.getMessage());
    return List.of();
  }

}
