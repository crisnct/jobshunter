package com.jobshunter.controller;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.EmailRequest;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.dto.exceptions.BusinessException;
import com.jobshunter.dto.exceptions.ValidationException;
import com.jobshunter.dto.geminiRequest.GeminiJobsPayload;
import com.jobshunter.dto.geminiRequest.GenerationConfig;
import com.jobshunter.dto.geminiRequest.GoogleSearchTool;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse;
import com.jobshunter.dto.gptRequest.GptJobsPayload;
import com.jobshunter.dto.gptRequest.tools.Tools;
import com.jobshunter.dto.gptResponse.GptCompletionResponse;
import com.jobshunter.dto.serpRequest.SearchWithSerpRequest;
import com.jobshunter.model.Job;
import com.jobshunter.model.PromptType;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.gemini.GeminiV1JobSearchImpl;
import com.jobshunter.service.clients.gpt.GptV1JobSearchImpl;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
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

  private final UserDataService userDataService;

  private final RestClient restClient;

  private final AiJobsClient<SearchWithSerpRequest, List<Job>> serpApi;

  private final TemplateRenderer templateRenderer;

  public TestController(
      EmailNotifierService emailNotifierService,
      UserDataService userDataService,
      ApplicationProperties properties,
      RestClient restClient,
      @Qualifier("JobsClientSerp") AiJobsClient<SearchWithSerpRequest, List<Job>> serpApi,
      TemplateRenderer templateRenderer
  ) {
    this.emailNotifierService = emailNotifierService;
    this.userDataService = userDataService;
    this.serpApi = serpApi;
    this.properties = properties;
    this.restClient = restClient;
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
    UserEntity user = userDataService.getUser(userDetails.getUsername())
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
    UserEntity user = userDataService.getUser(userDetails.getUsername())
        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
    log.info("Searching jobs for {}", user.getUsername());
    List<Job> jobs = serpApi.searchJobs(request);
    return ResponseEntity.ok(jobs);
  }

  @PostMapping(value = "/testGptModels", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  @RateLimiter(name = "gptLimiter")
  public ResponseEntity<?> testGPTModels(@Valid @RequestBody ModelsPayload modelsPayload) {
    List<String> modelsSupported = new ArrayList<>();
    for (String model : modelsPayload.models()) {
      GptJobsPayload payload = GptJobsPayload.builder()
          .model(model)
          .max_output_tokens(200)
          .addSystemPrompt("Act like an developer working at Open AI")
          .build();
      boolean supported = false;
      try {
        ResponseEntity<GptCompletionResponse> response = restClient.post()
            .uri(GptV1JobSearchImpl.DEFAULT_URI)
            .header("Authorization", "Bearer " + properties.getGpt().getApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .toEntity(GptCompletionResponse.class);
        if (response.getStatusCode().is2xxSuccessful()) {
          supported = true;
        }
      } catch (Throwable _) {
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
    return ResponseEntity.ok(modelsSupported);
  }

  @PostMapping(value = "/testGeminiModels", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  @RateLimiter(name = "geminiLimiter")
  public ResponseEntity<?> testGeminiModels(@Valid @RequestBody ModelsPayload modelsPayload) {
    List<String> modelsSupported = new ArrayList<>();
    for (String model : modelsPayload.models()) {
      GenerationConfig generationConfig = GenerationConfig.builder()
          .temperature(0.0)
          .maxOutputTokens(properties.getGemini().getMaxTokens())
          .build();

      GeminiJobsPayload payload = GeminiJobsPayload.builder()
          .addSystemInstruction(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_JOB_SEARCH))
          .addUserContent("Act like an developer working at Open AI")
          .generationConfig(generationConfig)
          .tools(List.of(new GoogleSearchTool()))
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
      } catch (Throwable _) {
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
    return ResponseEntity.ok(modelsSupported);
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
    GptJobsPayload gptPayload = GptJobsPayload.builder()
        .model(model)
        .max_output_tokens(2000)
        .addTools(Tools.builder().setDeepSearch().build())
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
          .header("Authorization", "Bearer " + properties.getGpt().getApiKey())
          .contentType(MediaType.APPLICATION_JSON)
          .body(gptPayload)
          .retrieve()
          .toEntity(GptCompletionResponse.class);
    } catch (Throwable e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  public record ModelsPayload(Set<String> models) {

  }

}
