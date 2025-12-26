package com.jobshunter.service.clients.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.ApplicationProperties;
import com.jobshunter.ApplicationProperties.Gemini;
import com.jobshunter.dto.geminiRequest.FileData;
import com.jobshunter.dto.geminiRequest.GeminiJobsPayload;
import com.jobshunter.dto.geminiRequest.GenerationConfig;
import com.jobshunter.dto.geminiRequest.Part;
import com.jobshunter.dto.geminiRequest.SafetySetting;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse.Candidate;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.JobScoreCalculatorClient;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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
@Component("GeminiScoreCalculator")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "true")
@RequiredArgsConstructor
public non-sealed class GeminiJobScoreCalculatorClientImpl implements JobScoreCalculatorClient<GeminiJobScoreRequest> {

  private static final String AI_MODEL = "gemini-2.5-flash-lite";

  private static final String FILES_URI = "https://generativelanguage.googleapis.com/v1beta";

  private static final String GEMINI_URI = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

  private final ApplicationProperties properties;

  private final RestClient restClient;

  private String calculateScoreSystemPrompt;

  private String calculateScoreUserPrompt;

  private final JsonMapper mapper;

  @PostConstruct
  @SuppressWarnings("DataFlowIssue")
  public void init() throws IOException {
    try (var inputStream = getClass().getClassLoader().getResourceAsStream(
        "prompts/gptScoreUserPrompt.txt")) {
      calculateScoreUserPrompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
    try (var inputStream = getClass().getClassLoader().getResourceAsStream(
        "prompts/gptScoreSystemPrompt.txt")) {
      calculateScoreSystemPrompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Override
  @RateLimiter(name = "geminiLimiter")
  public int computeScore(GeminiJobScoreRequest request) {
    try {
      Gemini config = properties.getGemini();
      GenerationConfig generationConfig = GenerationConfig.builder()
          .temperature(0.0)
          .maxOutputTokens(config.getMaxTokens())
          .build();

      FileData resume = new FileData(FILES_URI + "/" + request.getResumeFileId(), MediaType.APPLICATION_PDF_VALUE);
      FileData jobDescriptionFile = new FileData(FILES_URI + "/" + request.getJobDescriptionFileId(), MediaType.TEXT_PLAIN_VALUE);

      GeminiJobsPayload payload = GeminiJobsPayload.builder()
          .addSystemInstruction(calculateScoreSystemPrompt)
          .addUserContent(calculateScoreUserPrompt, List.of(resume, jobDescriptionFile))
          .generationConfig(generationConfig)
          .safetySettings(List.of(new SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_LOW_AND_ABOVE")))
          .build();

      GeminiGenerateContentResponse response = restClient.post()
          .uri(URI.create(String.format(GEMINI_URI, AI_MODEL, properties.getGemini().getApiKey())))
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(GeminiGenerateContentResponse.class);

      return extractScore(response);
    } catch (Exception e) {
      log.error("GEMINI job API call failed", e);
      return 0;
    }
  }

  private int extractScore(GeminiGenerateContentResponse response) throws JsonProcessingException {
    Optional<Part> item = response.candidates().stream()
        .flatMap((Function<Candidate, Stream<Part>>) candidate -> candidate.content().parts().stream()).findFirst();
    return item.map(part -> Integer.parseInt(part.text())).orElse(0);
  }

}
