package com.jobshunter.service.clients.gpt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.config.StringUtils;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobRoleEntity;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.CompanyDtoList;
import com.jobshunter.dto.GptSearchRequest;
import com.jobshunter.dto.exceptions.BusinessException;
import com.jobshunter.dto.gptRequest.GptJobsPayload;
import com.jobshunter.dto.gptRequest.Reasoning;
import com.jobshunter.dto.gptRequest.tools.Tools;
import com.jobshunter.dto.gptRequest.tools.UserLocation;
import com.jobshunter.dto.gptResponse.GptResponse;
import com.jobshunter.dto.gptResponse.JobSearchResponse;
import com.jobshunter.dto.gptResponse.OutputItem;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.AiSchemaType;
import com.jobshunter.model.Job;
import com.jobshunter.model.PromptType;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.UrlExtractor;
import com.jobshunter.service.application.cost.AiCostPublisher;
import com.jobshunter.service.application.cost.TokenEstimationGuard;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.AiJobsCompaniesClient;
import com.jobshunter.service.clients.DeleteConvAiClient;
import com.jobshunter.service.retry.RetryPolicies;
import com.jobshunter.service.retry.RetryTemplate;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.jsonwebtoken.lang.Collections;
import io.micrometer.core.annotation.Timed;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("JobsClientGPT")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "true")
@AllArgsConstructor
public non-sealed class GptV1JobSearchImpl implements AiJobsClient<GptSearchRequest>, AiJobsCompaniesClient<GptSearchRequest>, DeleteConvAiClient {

  public static final URI DEFAULT_URI = URI.create("https://api.openai.com/v1/responses");

  private final ApplicationProperties properties;

  private final RestClient restClient;

  private final JsonMapper mapper;

  private final UrlExtractor urlExtractor;

  private final RetryTemplate retryTemplate;

  private final TemplateRenderer templateRenderer;

  private final TokenEstimationGuard tokenEstimationGuard;

  private final AiCostPublisher costPublisher;

  @Override
  @Timed(value = "ai.api.search", extraTags = {"provider", "gpt", "operation", "search"})
  @CircuitBreaker(name = "gptCircuitBreaker", fallbackMethod = "fallbackSearch")
  @RateLimiter(name = "gptLimiter")
  @Bulkhead(name = "gptBulkhead")
  public AiClientResponse searchJobs(GptSearchRequest request) {
    return retryTemplate.execute(RetryPolicies.JOB_SEARCH, "GPT", () -> searchJobsOnce(request));
  }

  private AiClientResponse searchJobsOnce(GptSearchRequest request) {
    SearchJobOrder order = request.getOrder();

    UserLocation userLocation = new UserLocation();
    userLocation.setType("approximate");
    //This is country iso code, like RO
    userLocation.setCountry(request.getCountryIsoCode());
    userLocation.setCity(StringUtils.removeDiacritics(order.getUser().getCity()));

    GptJobsPayload payload = GptJobsPayload.builder(order.getModel())
        .store(request.getStoreConversation())
        .maxOutputTokens(15000)
        .reasoning(new Reasoning(REASONING_JOB_SEARCH))
        .previousResponseId(request.getPrevResponseId())
        .addTools(Tools.builder().setWebSearch().userLocation(userLocation).build())
        .instructions(templateRenderer.getPrompt(PromptType.SYSTEM_INSTRUCTIONS))
        .addSystemPrompt(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_JOB_SEARCH,
            "blacklist",
            properties.getJobsHunter().getBlacklist()
        ))
        .addUserPrompt(request.getUserPrompt())
        .setResponseSchema(templateRenderer.getSchema(AiSchemaType.GPT_JSON_SCHEMA_RESPONSE))
        .build();

    tokenEstimationGuard.assertFitsContext(payload);

    GptResponse response = restClient.post()
        .uri(DEFAULT_URI)
        .headers((h) -> h.setBearerAuth(properties.getGpt().getApiKey()))
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .body(GptResponse.class);

    //noinspection DataFlowIssue
    List<Job> jobs = extractJobs(response);
    AiClientResponse result = new AiClientResponse();
    result.setId(response.id());
    result.addAll(jobs);
    costPublisher.publishGpt(request.getOrder().getJobOrder().getId(), payload.aiModel(), response.usage());
    return result;
  }

  @Override
  @Timed(value = "ai.api.search", extraTags = {"provider", "gpt", "operation", "companies"})
  @CircuitBreaker(name = "gptCircuitBreaker", fallbackMethod = "fallbackSearchCompanies")
  @RateLimiter(name = "gptLimiter")
  @Bulkhead(name = "gptBulkhead")
  public List<CompanyDto> searchCompanies(GptSearchRequest request) {
    return retryTemplate.execute(RetryPolicies.COMPANY_SEARCH, "GPT", () -> searchCompaniesOnce(request));
  }

  /**
   * @Retry - is only for retrying when exception occurs. For the situations when result is empty it will be considered  retryTemplate
   */
  private List<CompanyDto> searchCompaniesOnce(GptSearchRequest request) {
    UserEntity user = request.getOrder().getUser();
    UserLocation userLocation = new UserLocation();
    userLocation.setType("approximate");
    //This is country iso code, like RO
    userLocation.setCountry(request.getCountryIsoCode());
    userLocation.setCity(StringUtils.removeDiacritics(user.getCity()));

    GptJobsPayload payload = GptJobsPayload.builder(request.getCompaniesModel())
        .store(false)
        .maxOutputTokens(15000)
        .addSystemPrompt(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_COMPANY_SEARCH,
            Map.of(
                "city", user.getCity(),
                "country", user.getCountry()
            )
        ))
        .addUserPrompt(templateRenderer.getPrompt(PromptType.USER_PROMPT_COMPANIES,
            Map.of(
                "city", user.getCity(),
                "country", user.getCountry(),
                "domain", user.getJobDomain()
            )))
        .setResponseSchema(templateRenderer.getSchema(AiSchemaType.GPT_JSON_COMPANY_SCHEMA_RESPONSE))
        .build();

    tokenEstimationGuard.assertFitsContext(payload);

    GptResponse response = restClient.post()
        .uri(DEFAULT_URI)
        .headers((h) -> h.setBearerAuth(properties.getGpt().getApiKey()))
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .body(GptResponse.class);

    costPublisher.publishGpt(request.getOrder().getJobOrder().getId(), payload.aiModel(), response.usage());

    return extractCompanies(response);
  }

  @Override
  @Timed(value = "ai.api.search", extraTags = {"provider", "gpt", "operation", "jobs-from-companies"})
  @CircuitBreaker(name = "gptCircuitBreaker", fallbackMethod = "fallbackSearchJobsFromCompanies")
  @RateLimiter(name = "gptLimiter")
  @Bulkhead(name = "gptBulkhead")
  public AiClientResponse searchJobsFromCompanies(GptSearchRequest request) {
    return retryTemplate.execute(RetryPolicies.JOB_SEARCH_BY_COMPANY, "GPT", () -> searchJobsFromCompanyOnce(request));
  }

  private AiClientResponse searchJobsFromCompanyOnce(GptSearchRequest request) {
    UserEntity user = request.getOrder().getUser();
    List<String> positions = user.getJobRoles().stream().map(UserJobRoleEntity::getJobRole).toList();

    GptJobsPayload payload = GptJobsPayload.builder(request.getDiscoveryModel())
        .temperature(0.15)
        .maxOutputTokens(15000)
        .store(request.getStoreConversation())
        .previousResponseId(request.getPrevResponseId())
        .addTools(Tools.builder().setWebSearch().build())
        .addSystemPrompt(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_JOBS_BY_COMPANY))
        .addUserPrompt(templateRenderer.getPrompt(PromptType.USER_PROMPT_JOB,
            Map.of(
                "company_name", request.getCompany().companyName(),
                "company_domain", URI.create(request.getCompany().officialWebsiteUrl()).getHost(),
                "positions", positions
            )
        ))
        .setResponseSchema(templateRenderer.getSchema(AiSchemaType.GPT_JSON_SCHEMA_RESPONSE))
        .build();

    tokenEstimationGuard.assertFitsContext(payload);

    GptResponse response = restClient.post()
        .uri(DEFAULT_URI)
        .headers((h) -> h.setBearerAuth(properties.getGpt().getApiKey()))
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .body(GptResponse.class);
    //noinspection DataFlowIssue
    List<Job> jobs = extractJobs(response);
    AiClientResponse result = new AiClientResponse();
    result.setId(response.id());
    result.addAll(jobs);
    costPublisher.publishGpt(request.getOrder().getJobOrder().getId(), payload.aiModel(), response.usage());
    return result;
  }

  @Override
  @CircuitBreaker(name = "gptCircuitBreaker", fallbackMethod = "fallbackDeleteConversation")
  @RateLimiter(name = "gptLimiter")
  @Bulkhead(name = "gptBulkhead")
  public void deleteConversation(String id) {
    restClient.delete()
        .uri(DEFAULT_URI + "/" + id)
        .headers((h) -> h.setBearerAuth(properties.getGpt().getApiKey()))
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            (request, response) -> {
              throw new BusinessException(HttpStatus.NOT_FOUND, "Delete failed: " + response.getStatusCode() + " for id " + id);
            }
        )
        .toBodilessEntity();
  }

  @SuppressWarnings("unused")
  private AiClientResponse fallbackSearchJobsFromCompanies(GptSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded fallbackSearchJobsFromCompanies: {}", getClass().getSimpleName(), t.getMessage(), t);
    return new AiClientResponse();
  }

  @SuppressWarnings("unused")
  private AiClientResponse fallbackSearch(GptSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded fallbackSearch: {}", getClass().getSimpleName(), t.getMessage(), t);
    return new AiClientResponse();
  }

  @SuppressWarnings("unused")
  private List<CompanyDto> fallbackSearchCompanies(GptSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded fallbackSearchCompanies: {}", getClass().getSimpleName(), t.getMessage(), t);
    return List.of();
  }

  protected List<Job> extractJobs(GptResponse response) {
    if (Collections.isEmpty(response.output())) {
      return List.of();
    }
    Optional<OutputItem> item = response.output().stream()
        .filter(p -> Objects.equals(p.type(), "message") && !p.content().isEmpty())
        .findAny();
    if (item.isPresent()) {
      final List<Job> jobs = new ArrayList<>();
      item.get().content().stream()
          .filter(c -> c.text().length() > 2)
          .filter(c -> Objects.equals("output_text", c.type()))
          .forEach(o -> {
            try {
              JobSearchResponse resp = mapper.readValue(o.text(), JobSearchResponse.class);
              jobs.addAll(resp.results().stream().map(p -> new Job(p.job_posting_url())).toList());
            } catch (Exception e) {
              log.warn("Exception for mapping jobs from GPT to response {}", e.getMessage());
              jobs.addAll(urlExtractor.parseJobs(o.text()));
            }
          });
      return jobs;
    } else {
      return java.util.Collections.emptyList();
    }
  }


  @SuppressWarnings("unused")
  private void fallbackDeleteConversation(String id, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
  }

  protected List<CompanyDto> extractCompanies(GptResponse response) {
    if (Collections.isEmpty(response.output())) {
      return List.of();
    }
    Optional<OutputItem> item = response.output().stream()
        .filter(p -> Objects.equals(p.type(), "message") && !p.content().isEmpty())
        .findAny();
    if (item.isPresent()) {
      List<CompanyDto> companiesAll = new ArrayList<>();
      item.get().content().stream()
          .filter(c -> c.text().length() > 2)
          .filter(c -> Objects.equals("output_text", c.type()))
          .forEach(o -> {
            try {
              CompanyDtoList companies = mapper.readValue(o.text(), CompanyDtoList.class);
              companiesAll.addAll(companies.results());
            } catch (JsonProcessingException e) {
              throw new RuntimeException(e);
            }
          });
      return companiesAll;
    } else {
      return List.of();
    }
  }
}
