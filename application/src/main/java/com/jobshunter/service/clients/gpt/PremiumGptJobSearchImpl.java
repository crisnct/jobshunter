package com.jobshunter.service.clients.gpt;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.config.ApplicationProperties.ModelSpecific;
import com.jobshunter.dto.GptCompletionResponse;
import com.jobshunter.dto.GptJobSearchRequest;
import com.jobshunter.dto.Input;
import com.jobshunter.dto.InputFile;
import com.jobshunter.dto.InputMessage;
import com.jobshunter.dto.Job;
import com.jobshunter.dto.Tools;
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
      GptJobsPayload payload = new GptJobsPayload(
          cfg.getModel(),
          properties.getGpt().getTemperature(),
          properties.getGpt().getMaxTokens(),
          List.of(new Tools(properties.getGpt().getToolsType())),
          List.of(
              new Input("system", List.of(new InputMessage("input_text", systemPrompt))),
              new Input("user", List.of(
                  new InputMessage("input_text", userPrompt),
                  new InputFile(fileId)
              ))
          )
      );

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
