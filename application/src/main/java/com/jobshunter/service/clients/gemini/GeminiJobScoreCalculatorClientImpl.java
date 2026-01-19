package com.jobshunter.service.clients.gemini;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.entities.UserRemoteCvEntity;
import com.jobshunter.dto.exceptions.ValidationException;
import com.jobshunter.dto.geminiRequest.FileData;
import com.jobshunter.dto.geminiRequest.GeminiJobsPayload;
import com.jobshunter.dto.geminiRequest.GenerationConfig;
import com.jobshunter.dto.geminiRequest.Part;
import com.jobshunter.dto.geminiRequest.ThinkingConfig;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse.Candidate;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.JobScoreRequest;
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
public non-sealed class GeminiJobScoreCalculatorClientImpl implements JobScoreCalculatorClient {

  private static final String GENERATE_CONTENT_URI = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

  private static final String FILES_URI = "https://generativelanguage.googleapis.com/%s";

  private final ApplicationProperties properties;

  private final RestClient restClient;

  private final TemplateRenderer templateRenderer;

  @Override
  @RateLimiter(name = "geminiLimiter")
  @Bulkhead(name = "geminiBulkhead")
  @CircuitBreaker(name = "geminiCircuitBreaker", fallbackMethod = "fallbackComputeScore")
  public int computeScore(JobScoreRequest request) {
    try {
      GenerationConfig generationConfig = GenerationConfig.builder(request.getModel())
          .maxOutputTokens(20)
          .thinkingConfig(new ThinkingConfig(1024))//how reasoning it is
          .build();

      UserRemoteCvEntity remoteCV = request.getUserCV().getUser().getRemoteCvs().stream()
          .filter(p -> p.getProvider() == EngineType.GEMINI).findAny()
          .orElseThrow(() -> new ValidationException("No GEMINI CV found for user " + request.getUserCV().getUser().getUsername()));

      FileData resume = new FileData(String.format(FILES_URI, remoteCV.getFileId()), MediaType.APPLICATION_PDF_VALUE);

      GeminiJobsPayload payload = GeminiJobsPayload.builder(request.getModel())
          .addSystemInstruction(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_MATCH_SCORE))
          .addUserContent(templateRenderer.getPrompt(PromptType.USER_PROMPT_MATCH_SCORE, "description", request.getJobDescription()),
              List.of(resume))
          .generationConfig(generationConfig)
          .build();

      GeminiGenerateContentResponse response = restClient.post()
          .uri(URI.create(String.format(GENERATE_CONTENT_URI, request.getModel().getModel(), properties.getGemini().getApiKey())))
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(GeminiGenerateContentResponse.class);

      //noinspection DataFlowIssue
      return extractScore(response);
    } catch (Exception e) {
      log.error("❌ GEMINI job API call failed", e);
      return 0;
    }
  }

  private int extractScore(GeminiGenerateContentResponse response) {
    Optional<Part> item = response.candidates().stream()
        .filter(p -> p.content() != null && p.content().parts() != null)
        .flatMap((Function<Candidate, Stream<Part>>) candidate -> candidate.content().parts().stream()).findFirst();
    return item.map(part -> Integer.parseInt(part.text())).orElse(0);
  }

  @SuppressWarnings("unused")
  private int fallbackComputeScore(JobScoreRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return 0;
  }

}
