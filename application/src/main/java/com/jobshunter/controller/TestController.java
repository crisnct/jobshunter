package com.jobshunter.controller;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.ModelsDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.dto.EmailRequest;
import com.jobshunter.dto.JobHuntResponse;
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
import com.jobshunter.dto.serpRequest.SearchWithSerpRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.AiSchemaType;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.PromptType;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import com.jobshunter.service.clients.AiJobsClient;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class TestController {

  private final EmailNotifierService emailNotifierService;

  private final ApplicationProperties properties;

  private final UserDBService userDBService;

  private final RestClient restClient;

  private final AiJobsClient<SearchWithSerpRequest, AiClientResponse> serpClient;

  private final TemplateRenderer templateRenderer;

  private final ModelsDBService modelsDBService;

  public TestController(
      EmailNotifierService emailNotifierService,
      UserDBService userDBService,
      ApplicationProperties properties,
      RestClient restClient,
      ModelsDBService modelsDBService,
      @Qualifier("JobsClientSerp") AiJobsClient<SearchWithSerpRequest, AiClientResponse> serpClient,
      TemplateRenderer templateRenderer
  ) {
    this.emailNotifierService = emailNotifierService;
    this.userDBService = userDBService;
    this.serpClient = serpClient;
    this.properties = properties;
    this.restClient = restClient;
    this.modelsDBService=modelsDBService;
    this.templateRenderer = templateRenderer;
  }

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

  @PostMapping(value = "/searchWithSerp", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> searchJobsWithSERP(
      @Valid
      @RequestBody
      SearchWithSerpRequest request,

      @AuthenticationPrincipal
      UserDetails userDetails
  ) {
    UserEntity user = userDBService.getUser(userDetails.getUsername())
        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
    log.info("Searching jobs for {}", user.getUsername());
    return ResponseEntity.ok(serpClient.searchJobs(request));
  }

  @PostMapping(value = "/testGptModels", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  @RateLimiter(name = "gptLimiter")
  public ResponseEntity<?> testGPTModels() {
    List<String> modelsSupported = new ArrayList<>();
    for (String model : GPT_MODELS) {
      AiModelEntity aiModel = modelsDBService.getModelById(EngineType.GPT, model).get();

      GptJobsPayload payload = GptJobsPayload.builder(aiModel)
          .model(model)
          .maxOutputTokens(200)
          //.addTools(Tools.builder().setWebSearch().build())
          //.reasoning(new Reasoning())
          //.store(true)
          //.previousResponseId("resp_04290e486a986c7b0069694046f22c8192ace2d3fe38c6f46a")
         // .temperature(0.2)
          //.addSystemPrompt("Act like a job search assistant and search jobs for me")
          //.setResponseSchema(templateRenderer.getSchema(AiSchemaType.GPT_JSON_SCHEMA_RESPONSE))
          .addUserPrompt("Search java developers jobs for me", "file-MD9RCwJjJ132DLKRosNQUt")
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
      if (!ret.isEmpty()){
        ret.append("\n");
      }
      if (modelsSupported.contains(model)){
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
      AiModelEntity aiModel = modelsDBService.getModelById(EngineType.GROK, model).get();
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
      if (!ret.isEmpty()){
        ret.append("\n");
      }
      if (modelsSupported.contains(model)){
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
      AiModelEntity aiModel = modelsDBService.getModelById(EngineType.GEMINI, model).get();
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
      if (!ret.isEmpty()){
        ret.append("\n");
      }
      if (modelsSupported.contains(model)){
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
    AiModelEntity aiModel = modelsDBService.getModelById(EngineType.GPT, model).get();
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
