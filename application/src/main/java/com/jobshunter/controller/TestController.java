package com.jobshunter.controller;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.ModelsDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.dto.EmailRequest;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.dto.JobScoreRequestDto;
import com.jobshunter.dto.exceptions.BusinessException;
import com.jobshunter.dto.exceptions.ValidationException;
import com.jobshunter.dto.geminiRequest.GeminiJobsPayload;
import com.jobshunter.dto.geminiRequest.GenerationConfig;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse;
import com.jobshunter.dto.gptRequest.GptJobsPayload;
import com.jobshunter.dto.gptRequest.tools.Tools;
import com.jobshunter.dto.gptResponse.GptResponse;
import com.jobshunter.dto.grokRequest.GrokJobsPayload;
import com.jobshunter.dto.grokResponse.GrokResponse;
import com.jobshunter.model.AiSchemaType;
import com.jobshunter.model.EngineSelection;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobScoreRequest;
import com.jobshunter.model.PromptType;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.TestService;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import com.jobshunter.service.application.processors.JobBodyExtractorProcessor;
import com.jobshunter.service.application.processors.JobFetchProcessor;
import com.jobshunter.service.clients.JobScoreCalculatorClient;
import com.jobshunter.service.clients.gemini.GeminiV1JobSearchImpl;
import com.jobshunter.service.clients.gpt.GptV1JobSearchImpl;
import com.jobshunter.service.clients.grok.GrokV1JobSearchImpl;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@Slf4j
@RestController
@RequestMapping("/api/test")
@PreAuthorize("hasRole('TEST')")
@AllArgsConstructor
public class TestController {

  private final EmailNotifierService emailNotifierService;

  private final ApplicationProperties properties;

  private final UserDBService userDBService;

  private final RestClient restClient;

  private final TemplateRenderer templateRenderer;

  private final ModelsDBService modelsDBService;

  private final JobFetchProcessor jobFetchProcessor;

  private final JobBodyExtractorProcessor jobBodyExtractorProcessor;

  private final TestService testService;

  @PostMapping(value = "/email/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> send(
      @Valid
      @ModelAttribute
      EmailRequest request
  ) {
    emailNotifierService.sendCustomEmail(request.getEmail(), request.getSubject(), request.getMessage(), request.getFile());
    return ResponseEntity.ok(Map.of("message", "Email sent successfully"));
  }

  @PostMapping(value = "/email/sendUsingTemplate", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> sendUsingTemplate(
      @Valid
      @RequestBody
      JobHuntResponse jobs,

      @AuthenticationPrincipal
      UserDetails userDetails
  ) {
    if (userDetails == null) {
      throw new ValidationException("Authentication required");
    }
    UserEntity user = userDBService.getUser(userDetails.getUsername())
        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
    emailNotifierService.sendUsingTemplate(jobs.jobsFound(), user);
    return ResponseEntity.ok(Map.of("message", "Email sent successfully"));
  }

  @PostMapping(value = "/testGptModels", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  @RateLimiter(name = "gptLimiter")
  public ResponseEntity<?> testGPTModels() {
    List<String> modelsSupported = new ArrayList<>();
    for (String model : GPT_MODELS) {
      AiModelEntity aiModel = modelsDBService.getModel(new EngineSelection(EngineType.GPT, model)).get();

      GptJobsPayload payload = GptJobsPayload.builder(aiModel)
          .model(model)
          .maxOutputTokens(16)
          //.addTools(Tools.builder().setWebSearch().build())
          //.reasoning(new Reasoning("low"))
          .store(false)
          //.previousResponseId("resp_04290e486a986c7b0069694046f22c8192ace2d3fe38c6f46a")
          .temperature(0.2)
          //.addSystemPrompt("Act like a job search assistant and search jobs for me")
          //.setResponseSchema(templateRenderer.getSchema(AiSchemaType.GPT_JSON_SCHEMA_RESPONSE))
          .addUserPrompt("Search java developers jobs for me", "file-GkWtEj4ffrWUxakq22yBLf")
          //.addDeveloperPrompt("Act like a stressed developer")
          .build();
      boolean supported = false;
      try {
        ResponseEntity<GptResponse> response = restClient.post()
            .uri(GptV1JobSearchImpl.DEFAULT_URI)
            .headers((h) -> h.setBearerAuth(properties.getGpt().getApiKey()))
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .toEntity(GptResponse.class);
        if (response.getStatusCode().is2xxSuccessful()) {
          supported = true;
        }
      } catch (Throwable e) {
        log.error("error {}", e.getMessage());
      }
      if (supported) {
        log.info("{} supported", model);
      } else {
        log.error("{} not supported", model);
      }
      if (supported) {
        modelsSupported.add(model);
      }
    }

    StringBuilder ret = new StringBuilder();
    for (String model : GPT_MODELS) {
      if (!ret.isEmpty()) {
        ret.append("\n");
      }
      if (modelsSupported.contains(model)) {
        ret.append(1);
      } else {
        ret.append(0);
      }
    }
    ret.append("\n\n\nModels supported:\n");
    ret.append(modelsSupported);
    return ResponseEntity.ok(ret.toString());
  }

  @PostMapping(value = "/testGrokModels", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  @RateLimiter(name = "grokLimiter")
  public ResponseEntity<?> testGROKModels() {
    List<String> modelsSupported = new ArrayList<>();
    for (String model : GROK_MODELS) {
      AiModelEntity aiModel = modelsDBService.getModel(new EngineSelection(EngineType.GROK, model)).get();
      GrokJobsPayload payload = GrokJobsPayload.builder(aiModel)
          .model(model)
          .maxOutputTokens(200)
          //.addTools(com.jobshunter.dto.grokRequest.tools.Tools.builder().setWebSearch().build())
          //.reasoning(new Reasoning())
          //.store(true)
          //.previousResponseId("65224478-5394-d462-c1a9-015ef2be2b0e")
          .temperature(0.2)
          //.addSystemPrompt("Act like a job search assistant and search jobs for me")
          //.setResponseSchema(templateRenderer.getSchema(AiSchemaType.GROK_JSON_SCHEMA_RESPONSE))
          //.reasoning(new com.jobshunter.dto.grokRequest.Reasoning("high"))
          .addUserPrompt("Search java developers jobs for me", "file_6cd02667-69ec-478b-b36e-343cc5cca014")
          //.addSystemPrompt("Act as search assistant")
          .build();
      boolean supported = false;
      try {
        ResponseEntity<GrokResponse> response = restClient.post()
            .uri(GrokV1JobSearchImpl.DEFAULT_URI)
            .headers((h) -> h.setBearerAuth(properties.getGrok().getApiKey()))
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .toEntity(GrokResponse.class);
        if (response.getStatusCode().is2xxSuccessful()) {
          supported = true;
        }
      } catch (Throwable e) {
        log.error("error {}", e.getMessage());
      }
      if (supported) {
        log.info("{} supported", model);
      } else {
        log.error("{} not supported", model);
      }
      if (supported) {
        modelsSupported.add(model);
      }
    }

    StringBuilder ret = new StringBuilder();
    for (String model : GROK_MODELS) {
      if (!ret.isEmpty()) {
        ret.append("\n");
      }
      if (modelsSupported.contains(model)) {
        ret.append(1);
      } else {
        ret.append(0);
      }
    }
    ret.append("\n\n\nModels supported:\n");
    ret.append(modelsSupported);
    return ResponseEntity.ok(ret.toString());
  }

  @PostMapping(value = "/testGeminiModels", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  @RateLimiter(name = "geminiLimiter")
  public ResponseEntity<?> testGeminiModels() {
    List<String> modelsSupported = new ArrayList<>();
    for (String model : GEMINI_MODELS) {
      AiModelEntity aiModel = modelsDBService.getModel(new EngineSelection(EngineType.GEMINI, model)).get();
      GenerationConfig generationConfig = GenerationConfig.builder(aiModel)
          .temperature(0.2)
          .responseMimeType("application/json")
          .responseJsonSchema(templateRenderer.getSchema(AiSchemaType.GEMINI_JSON_SCHEMA_RESPONSE))
          //.thinkingConfig(new ThinkingConfig(1024))
          .maxOutputTokens(500)
          .build();

      GeminiJobsPayload payload = GeminiJobsPayload.builder(aiModel)
//          .addSystemInstruction(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_JOB_SEARCH,
//              "blacklist",
//              properties.getJobsHunter().getBlacklist()
//          ))
          .addUserContent("[Context: Country=DE, Language=de] Act like an developer working at Gemini AI")
          //.addModelContent("Ok, got it. Now search java developer jobs for you")
          .generationConfig(generationConfig)
          //.tools(List.of(new GoogleSearchTool()))
          .build();
      boolean supported = false;
      try {
        ResponseEntity<GeminiGenerateContentResponse> response = restClient.post()
            .uri(URI.create(String.format(GeminiV1JobSearchImpl.GEMINI_URI, model,
                properties.getGemini().getApiKey())))
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .toEntity(GeminiGenerateContentResponse.class);
        if (response.getStatusCode().is2xxSuccessful()) {
          supported = true;
        }
      } catch (Throwable e) {
        log.error("error {}", e.getMessage());
      }
      if (supported) {
        log.info("{} supported", model);
      } else {
        log.error("{} not supported", model);
      }
      if (supported) {
        modelsSupported.add(model);
      }
    }

    StringBuilder ret = new StringBuilder();
    for (String model : GEMINI_MODELS) {
      if (!ret.isEmpty()) {
        ret.append("\n");
      }
      if (modelsSupported.contains(model)) {
        ret.append(1);
      } else {
        ret.append(0);
      }
    }
    ret.append("\n\n\nModels supported:\n");
    ret.append(modelsSupported);
    return ResponseEntity.ok(ret.toString());
  }

  @PostMapping(value = "/testGPT", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  @RateLimiter(name = "gptLimiter")
  public ResponseEntity<?> testGptSearch(
      @NotBlank
      @RequestParam("model")
      String model,

      @NotBlank
      @RequestParam("fileId")
      String fileId,

      @NotBlank
      @RequestParam("city")
      String city,

      @NotBlank
      @RequestParam("country")
      String country,

      @Valid
      @RequestBody
      String payload
  ) {
    AiModelEntity aiModel = modelsDBService.getModel(new EngineSelection(EngineType.GPT, model)).get();
    GptJobsPayload gptPayload = GptJobsPayload.builder(aiModel)
        .model(model)
        .maxOutputTokens(2000)
        .addTools(Tools.builder().setWebSearch().build())
        .addSystemPrompt(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_COMPANY_SEARCH,
            "city", city,
            "country", country,
            "timestamp", String.valueOf(Instant.now())
        ))
        .addUserPrompt(payload, fileId)
        .build();
    try {
      return restClient.post()
          .uri(GptV1JobSearchImpl.DEFAULT_URI)
          .headers((h) -> h.setBearerAuth(properties.getGpt().getApiKey()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(gptPayload)
          .retrieve()
          .toEntity(GptResponse.class);
    } catch (Throwable e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @PostMapping(value = "/score", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> computeJobScore(
      @Valid @RequestBody JobScoreRequestDto request,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    // Get authenticated user
    UserEntity user = userDBService.getUserCompleteInfo(userDetails.getUsername())
        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));

    // Validate user has CV
    if (user.getCv() == null) {
      throw new BusinessException(HttpStatus.BAD_REQUEST, "User has no CV uploaded");
    }

    // Parse engine provider
    EngineType engineType;
    try {
      engineType = EngineType.valueOf(request.engineProvider().toUpperCase());
      if (engineType == EngineType.SERP) {
        throw new ValidationException("Invalid engine provider. Must be GPT, GEMINI, or GROK");
      }
    } catch (IllegalArgumentException e) {
      throw new ValidationException("Invalid engine provider. Must be GPT, GEMINI, or GROK");
    }

    // Fetch HTML from job URL using JobFetchProcessor
    Job job = new Job(0, request.jobUrl(), "SCORE_ENDPOINT");
    JobContext jobContext = new JobContext(job, user);

    try {
      jobContext = jobFetchProcessor.processAsync(jobContext);
      if (jobContext.isFailed() || !jobContext.hasFetchResult()) {
        throw new BusinessException(HttpStatus.BAD_REQUEST,
            "Failed to fetch job URL: " + (jobContext.getFinalizationMessage() != null
                ? jobContext.getFinalizationMessage() : request.jobUrl()));
      }
    } catch (Exception e) {
      log.error("Error fetching job URL: {}", e.getMessage());
      throw new BusinessException(HttpStatus.BAD_REQUEST, "Failed to fetch job URL: " + e.getMessage());
    }

    // Extract job description text from HTML using JobBodyExtractorProcessor
    try {
      jobContext = jobBodyExtractorProcessor.processAsync(jobContext);
      if (jobContext.isFailed() || jobContext.getBody() == null) {
        throw new BusinessException(HttpStatus.BAD_REQUEST,
            "Failed to extract job description: " + (jobContext.getFinalizationMessage() != null
                ? jobContext.getFinalizationMessage() : "Unknown error"));
      }
    } catch (Exception e) {
      log.error("Error extracting job description: {}", e.getMessage());
      throw new BusinessException(HttpStatus.BAD_REQUEST, "Failed to extract job description: " + e.getMessage());
    }

    String jobDescription = jobContext.getBody();

    // Get model entity
    AiModelEntity aiModel = modelsDBService.getModel(new EngineSelection(engineType, request.engineModel()))
        .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Model not found: " + request.engineModel()));

    // Select appropriate calculator client
    JobScoreCalculatorClient calculator = testService.getScoreCalculator(engineType);

    // Create JobScoreRequest and compute score
    JobScoreRequest scoreRequest = new JobScoreRequest(aiModel, jobDescription, user.getCv());
    int score = calculator.computeScore(scoreRequest);

    return ResponseEntity.ok(Map.of("score", score));
  }

  public static final Set<String> GROK_MODELS =
      new LinkedHashSet<>(List.of(
          "grok-2-1212",
          "grok-2-image-1212",
          "grok-2-vision-1212",
          "grok-3",
          "grok-3-mini",
          "grok-4-0709",
          "grok-4-1-fast-non-reasoning",
          "grok-4-1-fast-reasoning",
          "grok-4-fast-non-reasoning",
          "grok-4-fast-reasoning",
          "grok-code-fast-1"
      ));

  public static final Set<String> GEMINI_MODELS =
      new LinkedHashSet<>(List.of(
          "gemini-2.0-flash",
          "gemini-2.0-flash-001",
          "gemini-2.0-flash-exp",
          "gemini-2.0-flash-lite",
          "gemini-2.0-flash-lite-001",
          "gemini-2.5-flash",
          "gemini-2.5-flash-lite",
          "gemini-2.5-pro"
      ));

  private static final Set<String> GPT_MODELS = new LinkedHashSet<>(List.of(
      "chatgpt-4o-latest",
      "gpt-3.5-turbo",
      "gpt-3.5-turbo-0125",
      "gpt-3.5-turbo-1106",
      "gpt-4",
      "gpt-4-0125-preview",
      "gpt-4-0613",
      "gpt-4-1106-preview",
      "gpt-4-turbo",
      "gpt-4-turbo-2024-04-09",
      "gpt-4-turbo-preview",
      "gpt-4.1",
      "gpt-4.1-2025-04-14",
      "gpt-4.1-mini",
      "gpt-4.1-mini-2025-04-14",
      "gpt-4.1-nano",
      "gpt-4.1-nano-2025-04-14",
      "gpt-4o",
      "gpt-4o-2024-05-13",
      "gpt-4o-2024-08-06",
      "gpt-4o-2024-11-20",
      "gpt-4o-mini",
      "gpt-4o-mini-2024-07-18",
      "gpt-5",
      "gpt-5-2025-08-07",
      "gpt-5-chat-latest",
      "gpt-5-codex",
      "gpt-5-mini",
      "gpt-5-mini-2025-08-07",
      "gpt-5-nano",
      "gpt-5-nano-2025-08-07",
      "gpt-5-pro",
      "gpt-5-pro-2025-10-06",
      "gpt-5.1",
      "gpt-5.1-2025-11-13",
      "gpt-5.1-chat-latest",
      "gpt-5.1-codex",
      "gpt-5.1-codex-max",
      "gpt-5.1-codex-mini",
      "gpt-5.2",
      "gpt-5.2-2025-12-11",
      "gpt-5.2-chat-latest",
      "gpt-5.2-pro",
      "gpt-5.2-pro-2025-12-11",
      "o1",
      "o1-2024-12-17",
      "o1-pro",
      "o1-pro-2025-03-19",
      "o3",
      "o3-2025-04-16",
      "o3-mini",
      "o3-mini-2025-01-31",
      "o4-mini",
      "o4-mini-2025-04-16"
  ));

}
