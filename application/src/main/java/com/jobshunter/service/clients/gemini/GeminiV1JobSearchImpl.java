package com.jobshunter.service.clients.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobRoleEntity;
import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.CompanyDtoList;
import com.jobshunter.dto.geminiRequest.FileData;
import com.jobshunter.dto.geminiRequest.GeminiJobsPayload;
import com.jobshunter.dto.geminiRequest.GenerationConfig;
import com.jobshunter.dto.geminiRequest.GoogleSearchTool;
import com.jobshunter.dto.geminiRequest.Part;
import com.jobshunter.dto.geminiRequest.ThinkingConfig;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse.Candidate;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse.UsageMetadata;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.AiSchemaType;
import com.jobshunter.model.Job;
import com.jobshunter.model.PromptType;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.UrlExtractor;
import com.jobshunter.service.application.cost.AiRequestCostEvent;
import com.jobshunter.service.application.cost.TokenEstimationGuard;
import com.jobshunter.service.application.cost.TokensConsumedMapper;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.AiJobsCompaniesClient;
import com.jobshunter.service.clients.DeleteConvAiClient;
import com.jobshunter.service.retry.RetryPolicies;
import com.jobshunter.service.retry.RetryTemplate;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.annotation.Timed;
import jakarta.annotation.Nonnull;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("JobsClientGemini")
@PackageExpected("com.jobshunter.service.clients.gemini")
@ConditionalOnProperty(name = "gemini.enabled", havingValue = "true")
@AllArgsConstructor
public non-sealed class GeminiV1JobSearchImpl implements AiJobsClient, AiJobsCompaniesClient, DeleteConvAiClient {

  public static final String GEMINI_URI = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

  private static final String FILES_URI = "https://generativelanguage.googleapis.com/%s";

  private final ApplicationProperties properties;

  private final RestClient restClient;

  private final UrlExtractor urlExtractor;

  private final TemplateRenderer templateRenderer;

  private final RetryTemplate retryTemplate;

  private final JsonMapper mapper;

  private final TokenEstimationGuard tokenEstimationGuard;

  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Timed(value = "ai.api.search", extraTags = {"provider", "gemini", "operation", "search"})
  @CircuitBreaker(name = "geminiCircuitBreaker", fallbackMethod = "fallbackSearch")
  @Bulkhead(name = "geminiBulkhead")
  @RateLimiter(name = "geminiLimiter")
  public AiClientResponse searchJobs(AIJobSearchRequest request) {
    return retryTemplate.execute(RetryPolicies.JOB_SEARCH, "GEMINI", () -> searchJobsOnce(request));
  }

  @Nonnull
  private AiClientResponse searchJobsOnce(AIJobSearchRequest request) {
    AiModelEntity model = request.getOrder().getModel();

    GenerationConfig generationConfig = GenerationConfig.builder(model)
        .temperature(0.05)
        .maxOutputTokens(15000)
        .thinkingConfig(new ThinkingConfig(256))//how reasoning it is
        .build();

    String systemPrompt = templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_JOB_SEARCH,
        "blacklist",
        properties.getJobsHunter().getBlacklist()
    );

    FileData resume = new FileData(String.format(FILES_URI, request.getFileId()), MediaType.APPLICATION_PDF_VALUE);

    GeminiJobsPayload payload = GeminiJobsPayload.builder(model)
        .generationConfig(generationConfig)
        .tools(List.of(new GoogleSearchTool()))
        .addSystemInstruction(systemPrompt)
        .addUserContent(request.getUserPrompt(), List.of(resume))
        .build();

    tokenEstimationGuard.assertFitsContext(payload);

    GeminiGenerateContentResponse response = restClient.post()
        .uri(URI.create(String.format(GEMINI_URI, model.getModel(), properties.getGemini().getApiKey())))
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .body(GeminiGenerateContentResponse.class);

    List<Job> jobs = extractContentList(response);
    AiClientResponse result = new AiClientResponse();
    result.addAll(jobs);
    //noinspection DataFlowIssue
    result.setId(response.responseId());
    UsageMetadata usage = response.usageMetadata();
    if (usage != null) {
      eventPublisher.publishEvent(new AiRequestCostEvent(
          this,
          request.getOrder() != null ? request.getOrder().getJobOrder().getId() : -1,
          payload.aiModel(),
          TokensConsumedMapper.fromGemini(usage))
      );
    }
    return result;
  }

  @SuppressWarnings("unused")
  private AiClientResponse fallbackSearch(AIJobSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return new AiClientResponse();
  }

  @Override
  @Timed(value = "ai.api.search", extraTags = {"provider", "gemini", "operation", "companies"})
  @CircuitBreaker(name = "geminiCircuitBreaker", fallbackMethod = "fallbackSearchCompanies")
  @Bulkhead(name = "geminiBulkhead")
  @RateLimiter(name = "geminiLimiter")
  public List<CompanyDto> searchCompanies(AIJobSearchRequest request) {
    return retryTemplate.execute(RetryPolicies.COMPANY_SEARCH, "GEMINI", () -> searchCompaniesOnce(request));
  }

  private List<CompanyDto> searchCompaniesOnce(AIJobSearchRequest request) {
    AiModelEntity model = request.getOrder().getModel();
    UserEntity user = request.getOrder().getUser();

    GenerationConfig generationConfig = GenerationConfig.builder(model)
        .maxOutputTokens(10000)
        .thinkingConfig(new ThinkingConfig(512))//how reasoning it is
        .responseJsonSchema(templateRenderer.getSchema(AiSchemaType.GEMINI_JSON_COMPANY_SCHEMA_RESPONSE))
        .build();

    GeminiJobsPayload payload = GeminiJobsPayload.builder(model)
        .generationConfig(generationConfig)
        .addSystemInstruction(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_COMPANY_SEARCH,
            Map.of(
                "city", user.getCity(),
                "country", user.getCountry()
            )
        ))
        .addUserContent(templateRenderer.getPrompt(PromptType.USER_PROMPT_COMPANIES,
            Map.of(
                "city", user.getCity(),
                "country", user.getCountry(),
                "domain", user.getJobDomain()
            )))
        .build();

    tokenEstimationGuard.assertFitsContext(payload);

    GeminiGenerateContentResponse response = restClient.post()
        .uri(URI.create(String.format(GEMINI_URI, model.getModel(), properties.getGemini().getApiKey())))
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .body(GeminiGenerateContentResponse.class);

    UsageMetadata usage = response.usageMetadata();
    if (usage != null) {
      eventPublisher.publishEvent(new AiRequestCostEvent(
          this,
          request.getOrder().getJobOrder().getId(),
          payload.aiModel(),
          TokensConsumedMapper.fromGemini(usage))
      );
    }

    return extractCompanies(response);
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

  private List<CompanyDto> extractCompanies(GeminiGenerateContentResponse response) {
    if (response == null || response.candidates() == null) {
      return List.of();
    }
    return response.candidates().stream()
        .map(Candidate::content)
        .filter(c -> c != null && c.parts() != null)
        .flatMap(c -> c.parts().stream())
        .map(Part::text)
        .filter(text -> text != null && text.length() > 2)
        .flatMap((Function<String, Stream<CompanyDto>>) s -> {
              try {
                CompanyDtoList companies = mapper.readValue(s, CompanyDtoList.class);
                return companies.results().stream();
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            }
        )
        .toList();
  }

  @Override
  @Timed(value = "ai.api.search", extraTags = {"provider", "gemini", "operation", "jobs-from-companies"})
  @CircuitBreaker(name = "geminiCircuitBreaker", fallbackMethod = "fallbackSearchJobsFromCompanies")
  @RateLimiter(name = "geminiLimiter")
  @Bulkhead(name = "geminiBulkhead")
  public AiClientResponse searchJobsFromCompanies(AIJobSearchRequest request) {
    return retryTemplate.execute(RetryPolicies.JOB_SEARCH_BY_COMPANY, "GEMINI", () -> searchJobsFromCompanyOnce(request));
  }

  private AiClientResponse searchJobsFromCompanyOnce(AIJobSearchRequest request) {
    UserEntity user = request.getOrder().getUser();
    AiModelEntity model = request.getOrder().getModel();
    List<String> positions = user.getJobRoles().stream().map(UserJobRoleEntity::getJobRole).toList();

    GenerationConfig generationConfig = GenerationConfig.builder(model)
        .temperature(0.15)
        .maxOutputTokens(10000)
        .thinkingConfig(new ThinkingConfig(256))//how reasoning it is
        .build();

    GeminiJobsPayload payload = GeminiJobsPayload.builder(model)
        .generationConfig(generationConfig)
        .tools(List.of(new GoogleSearchTool()))
        .addSystemInstruction(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_JOBS_BY_COMPANY))
        .addUserContent(templateRenderer.getPrompt(PromptType.USER_PROMPT_JOB,
            Map.of(
                "company_name", request.getCompany().companyName(),
                "company_domain", URI.create(request.getCompany().officialWebsiteUrl()).getHost(),
                "positions", positions
            )
        ))
        .build();

    tokenEstimationGuard.assertFitsContext(payload);

    GeminiGenerateContentResponse response = restClient.post()
        .uri(URI.create(String.format(GEMINI_URI, model.getModel(), properties.getGemini().getApiKey())))
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .body(GeminiGenerateContentResponse.class);

    List<Job> jobs = extractContentList(response);
    AiClientResponse result = new AiClientResponse();
    result.addAll(jobs);
    //noinspection DataFlowIssue
    result.setId(response.responseId());
    UsageMetadata usage = response.usageMetadata();
    if (usage != null) {
      eventPublisher.publishEvent(new AiRequestCostEvent(
          this,
          request.getOrder().getJobOrder().getId(),
          payload.aiModel(),
          TokensConsumedMapper.fromGemini(usage))
      );
    }
    return result;
  }

  @Override
  public void deleteConversation(String id) {
    //nothing to delete for gemini
  }

  public List<CompanyDto> fallbackSearchCompanies(AIJobSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded, fallbackSearchCompanies: {}", getClass().getSimpleName(), t.getMessage());
    return List.of();
  }

  public AiClientResponse fallbackSearchJobsFromCompanies(AIJobSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded, fallbackSearchJobsFromCompanies: {}", getClass().getSimpleName(), t.getMessage());
    return new AiClientResponse();
  }

}
