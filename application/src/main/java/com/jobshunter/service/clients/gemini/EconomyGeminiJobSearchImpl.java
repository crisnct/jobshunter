package com.jobshunter.service.clients.gemini;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.ApplicationProperties.ModelSpecific;
import com.jobshunter.dto.Job;
import com.jobshunter.dto.geminiRequest.GeminiJobSearchRequest;
import com.jobshunter.dto.geminiRequest.GeminiJobsPayload;
import com.jobshunter.dto.geminiRequest.GenerationConfig;
import com.jobshunter.dto.geminiRequest.GoogleSearchTool;
import com.jobshunter.dto.geminiRequest.SafetySetting;
import com.jobshunter.dto.geminiRequest.ThinkingConfig;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.AiJobsClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("EconomyJobsClientGemini")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gemini.enabled", havingValue = "true")
public non-sealed class EconomyGeminiJobSearchImpl extends AbstractGeminiApiClient
    implements AiJobsClient<GeminiJobSearchRequest, List<Job>> {

  private static final String GEMINI_URI = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

  @Autowired
  private ApplicationProperties properties;

  @Autowired
  private RestClient restClient;

  @Override
  public ModelSpecific getConfig() {
    return properties.getGemini().getEconomy();
  }

  @Override
  @CircuitBreaker(name = "geminiEconomy", fallbackMethod = "fallbackSearch")
  @Bulkhead(name = "geminiEconomyBulkhead", type = Bulkhead.Type.SEMAPHORE)
  @RateLimiter(name = "geminiLimiter")
  public List<Job> searchJobs(GeminiJobSearchRequest request) {
    try {
      GenerationConfig generationConfig = GenerationConfig.builder()
          .temperature(0.0)
          .maxOutputTokens(properties.getGemini().getMaxTokens())
          .thinkingConfig(new ThinkingConfig(2048))
          .build();

      GeminiJobsPayload payload = GeminiJobsPayload.builder()
          .addSystemInstruction(getJobsSystemPrompt())
          .addUserContent(request.getPrompt().getPrompt(), "application/pdf", request.getBase64CV())
          .generationConfig(generationConfig)
          .tools(List.of(new GoogleSearchTool()))
          .safetySettings(List.of(new SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_LOW_AND_ABOVE")))
          .build();

      GeminiGenerateContentResponse response = restClient.post()
          .uri(URI.create(String.format(GEMINI_URI, getConfig().getModel(), properties.getGemini().getApiKey())))
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(GeminiGenerateContentResponse.class);

      return extractJobs(response);
    } catch (Exception e) {
      log.error("Gemini job API call failed", e);
      return List.of();
    }
  }

  @SuppressWarnings("unused")
  private List<Job> fallbackSearch(GeminiJobSearchRequest request, Throwable t) {
    log.error("Economy Gemini call short-circuited/bulkheaded: {}", t.getMessage());
    return List.of();
  }

}
