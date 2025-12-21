package com.jobshunter.service.clients.gpt;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.config.ApplicationProperties.ModelSpecific;
import com.jobshunter.dto.Job;
import com.jobshunter.dto.gptRequest.GptJobSearchRequest;
import com.jobshunter.dto.gptRequest.GptJobsPayload;
import com.jobshunter.dto.gptRequest.Reasoning;
import com.jobshunter.dto.gptRequest.tools.Tools;
import com.jobshunter.dto.gptResponse.GptCompletionResponse;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.PremiumGptClient;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "jobshunter.useDummyData", havingValue = "false")
public non-sealed class PremiumGptJobSearchImpl extends AbstractGptApiClient
    implements PremiumGptClient<GptJobSearchRequest, List<Job>> {

  private static final URI DEFAULT_URI = URI.create("https://api.openai.com/v1/responses");

  @Autowired
  private RestClient restClient;

  @Autowired
  private ApplicationProperties properties;

  @Override
  public ModelSpecific getConfig() {
    return properties.getGpt().getPremium();
  }

  @Override
  public List<Job> searchWithModel(String systemPrompt, String userPrompt, ModelSpecific cfg, String fileId) {
    try {
      GptJobsPayload payload = GptJobsPayload.builder()
          .model(cfg.getModel())
          .reasoning(new Reasoning("high"))
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
      log.error("ChatGPT job API call failed: {}", e.getMessage());
      return List.of();
    }
  }

}
