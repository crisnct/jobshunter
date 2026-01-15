package com.jobshunter.service.clients.gemini;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.geminiRequest.GeminiJobsPayload;
import com.jobshunter.dto.geminiRequest.GenerationConfig;
import com.jobshunter.dto.geminiRequest.GoogleSearchTool;
import com.jobshunter.dto.geminiRequest.Part;
import com.jobshunter.dto.geminiRequest.ThinkingConfig;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse.Candidate;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.GeminiJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.model.PromptType;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.UrlExtractor;
import com.jobshunter.service.clients.AiJobsClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("JobsClientGemini")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gemini.enabled", havingValue = "true")
@AllArgsConstructor
public non-sealed class GeminiV1JobSearchImpl implements AiJobsClient<GeminiJobSearchRequest, AiClientResponse> {

  public static final String GEMINI_URI = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
  private static final double DEFAULT_TEMPERATURE = 0.3;
  private final ApplicationProperties properties;

  private final RestClient restClient;

  private final UrlExtractor urlExtractor;

  private final TemplateRenderer templateRenderer;

  @Override
  @CircuitBreaker(name = "geminiCircuitBreaker", fallbackMethod = "fallbackSearch")
  @Bulkhead(name = "geminiBulkhead")
  @RateLimiter(name = "geminiLimiter")
  public AiClientResponse searchJobs(GeminiJobSearchRequest request) {
    try {
      GenerationConfig generationConfig = GenerationConfig.builder()
          .temperature(DEFAULT_TEMPERATURE)
          .maxOutputTokens(3500)
          .thinkingConfig(new ThinkingConfig(2048))
          .build();

      GeminiJobsPayload payload = GeminiJobsPayload.builder()
          .addSystemInstruction(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_JOB_SEARCH,
              "blacklist",
              properties.getJobsHunter().getBlacklist()
          ))
          .addUserContent(request.getUserPrompt(), "application/pdf", request.getBase64CV())
          .generationConfig(generationConfig)
          .tools(List.of(new GoogleSearchTool()))
          .build();

      GeminiGenerateContentResponse response = restClient.post()
          .uri(URI.create(String.format(GEMINI_URI, request.getEngineSelection().model(),
              properties.getGemini().getApiKey())))
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(GeminiGenerateContentResponse.class);

      List<Job> jobs = extractContentList(response);
      AiClientResponse result = new AiClientResponse();
      result.addAll(jobs);
      //noinspection DataFlowIssue
      result.setId(response.responseId());
      return result;
    } catch (Exception e) {
      log.error("Gemini job API call failed", e);
      return new AiClientResponse();
    }
  }

  @Override
  public List<CompanyDto> searchCompanies(GeminiJobSearchRequest request) {
    return List.of();
  }

  @Override
  public AiClientResponse searchJobsFromCompanies(GeminiJobSearchRequest request, List<CompanyDto> group) {
    return new AiClientResponse();
  }

  @SuppressWarnings("unused")
  private AiClientResponse fallbackSearch(GeminiJobSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return new AiClientResponse();
  }

  protected List<Job> extractContentList(GeminiGenerateContentResponse response) {
    if (response == null || response.candidates() == null) {
      return List.of();
    }
    return response.candidates().stream()
        .map(Candidate::content)
        .filter(c -> c != null && c.parts() != null)
        .flatMap(c -> c.parts().stream())
        .map(Part::text)
        .filter(text -> text != null && text.length() > 2)
        .flatMap((Function<String, Stream<Job>>) s -> urlExtractor.parseJobs(s).stream())
        .toList();
  }
}
