package com.jobshunter.service.clients.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jobshunter.ApplicationProperties;
import com.jobshunter.ApplicationProperties.Gemini;
import com.jobshunter.dto.geminiRequest.FileData;
import com.jobshunter.dto.geminiRequest.GeminiJobsPayload;
import com.jobshunter.dto.geminiRequest.GenerationConfig;
import com.jobshunter.dto.geminiRequest.Part;
import com.jobshunter.dto.geminiRequest.SafetySetting;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse.Candidate;
import com.jobshunter.model.GeminiJobScoreRequest;
import com.jobshunter.model.PromptType;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.clients.JobScoreCalculatorClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("GeminiJobScoreCalculator")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gemini.enabled", havingValue = "true")
@RequiredArgsConstructor
public non-sealed class GeminiJobScoreCalculatorClientImpl implements JobScoreCalculatorClient<GeminiJobScoreRequest> {

  private static final String AI_MODEL = "gemini-2.5-pro";

  private static final String FILES_URI = "https://generativelanguage.googleapis.com/v1beta";

  private static final String GEMINI_URI = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

  private final ApplicationProperties properties;

  private final RestClient restClient;

  private final TemplateRenderer templateRenderer;

  @Override
  @RateLimiter(name = "geminiLimiter")
  @Bulkhead(name = "geminiBulkhead")
  @CircuitBreaker(name = "geminiCircuitBreaker", fallbackMethod = "fallbackComputeScore")
  public int computeScore(GeminiJobScoreRequest request) {
    try {
      GenerationConfig generationConfig = GenerationConfig.builder()
          .temperature(0.0)
          .maxOutputTokens(5)
          .build();

      FileData resume = new FileData(FILES_URI + "/" + request.getResumeFileId(), MediaType.APPLICATION_PDF_VALUE);

      GeminiJobsPayload payload = GeminiJobsPayload.builder()
          .addSystemInstruction(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_MATCH_SCORE))
          .addUserContent(templateRenderer.getPrompt(PromptType.USER_PROMPT_MATCH_SCORE, "description", request.getJobDescription()),
              List.of(resume))
          .generationConfig(generationConfig)
          .build();

      GeminiGenerateContentResponse response = restClient.post()
          .uri(URI.create(String.format(GEMINI_URI, AI_MODEL, properties.getGemini().getApiKey())))
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(GeminiGenerateContentResponse.class);

      return extractScore(response);
    } catch (Exception e) {
      log.error("❌ GEMINI job API call failed", e);
      return 0;
    }
  }

  private int extractScore(GeminiGenerateContentResponse response) throws JsonProcessingException {
    Optional<Part> item = response.candidates().stream()
        .flatMap((Function<Candidate, Stream<Part>>) candidate -> candidate.content().parts().stream()).findFirst();
    return item.map(part -> Integer.parseInt(part.text())).orElse(0);
  }

  @SuppressWarnings("unused")
  private int fallbackComputeScore(GeminiJobScoreRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return 0;
  }

}
