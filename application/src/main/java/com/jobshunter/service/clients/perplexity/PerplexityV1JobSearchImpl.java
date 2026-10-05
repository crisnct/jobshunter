package com.jobshunter.service.clients.perplexity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.config.ApplicationProperties.Perplexity;
import com.jobshunter.config.StringUtils;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobRoleEntity;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.CompanyDtoList;
import com.jobshunter.dto.PerplexitySearchRequest;
import com.jobshunter.dto.TokenEstimationResult;
import com.jobshunter.dto.perplexityRequest.PerplexityJobsPayload;
import com.jobshunter.dto.perplexityRequest.Reasoning;
import com.jobshunter.dto.perplexityRequest.tools.FetchUrlTool;
import com.jobshunter.dto.perplexityRequest.tools.UserLocation;
import com.jobshunter.dto.perplexityRequest.tools.WebSearchFilters;
import com.jobshunter.dto.perplexityRequest.tools.WebSearchTool;
import com.jobshunter.dto.perplexityResponse.ContentItem;
import com.jobshunter.dto.perplexityResponse.JobResult;
import com.jobshunter.dto.perplexityResponse.JobSearchResponse;
import com.jobshunter.dto.perplexityResponse.OutputItem;
import com.jobshunter.dto.perplexityResponse.PerplexityResponse;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.AiSchemaType;
import com.jobshunter.model.Job;
import com.jobshunter.model.PromptType;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.UrlExtractor;
import com.jobshunter.service.application.cost.AiCostPublisher;
import com.jobshunter.service.application.cost.TokenEstimationGuard;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.AiJobsCompaniesClient;
import com.jobshunter.service.retry.RetryPolicies;
import com.jobshunter.service.retry.RetryTemplate;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.annotation.Timed;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Perplexity Agent API client ({@code POST /v1/responses}, OpenAI Responses format) used for job discovery.
 * <p>
 * The web search runs on Perplexity's servers, so one HTTP call is the whole agent loop (bounded by {@code max_steps}). Conversations continue
 * through {@code previous_response_id}; there is no delete endpoint, hence this client is not a {@code DeleteConvAiClient}.
 */
@Slf4j
@Component("JobsClientPERPLEXITY")
@PackageExpected("com.jobshunter.service.clients.perplexity")
@ConditionalOnProperty(name = "perplexity.enabled", havingValue = "true")
@AllArgsConstructor
public non-sealed class PerplexityV1JobSearchImpl
    implements AiJobsClient<PerplexitySearchRequest>, AiJobsCompaniesClient<PerplexitySearchRequest> {

  static final String PROVIDER = "PERPLEXITY";
  static final int MAX_OUTPUT_TOKENS = 15000;
  static final int COMPANY_CAREERS_MAX_URLS = 3;

  /** Applicant tracking systems that host the postings of most companies; added to the allowlist of the per-company search. */
  static final List<String> ATS_DOMAINS = List.of(
      "greenhouse.io", "lever.co", "myworkdayjobs.com", "smartrecruiters.com", "ashbyhq.com", "workable.com",
      "bamboohr.com", "jobvite.com", "icims.com", "taleo.net", "recruitee.com");

  private final ApplicationProperties properties;

  private final RestClient restClient;

  private final RetryTemplate retryTemplate;

  private final JsonMapper mapper;

  private final UrlExtractor urlExtractor;

  private final TemplateRenderer templateRenderer;

  private final TokenEstimationGuard tokenEstimationGuard;

  private final AiCostPublisher costPublisher;

  private final PerplexityUrlVerifier urlVerifier;

  // ---------------------------------------------------------------------------
  // Search by prompt
  // ---------------------------------------------------------------------------

  @Override
  @Timed(value = "ai.api.search", extraTags = {"provider", "perplexity", "operation", "search"})
  @CircuitBreaker(name = "perplexityCircuitBreaker", fallbackMethod = "fallbackSearch")
  @RateLimiter(name = "perplexityLimiter")
  @Bulkhead(name = "perplexityBulkhead")
  public AiClientResponse searchJobs(PerplexitySearchRequest request) {
    return retryTemplate.execute(RetryPolicies.JOB_SEARCH, PROVIDER, () -> searchJobsOnce(request));
  }

  private AiClientResponse searchJobsOnce(PerplexitySearchRequest request) {
    Perplexity config = properties.getPerplexity();
    PerplexityJobsPayload payload = PerplexityJobsPayload.builder(request.getOrder().getModel())
        .reasoning(new Reasoning(REASONING_JOB_SEARCH))
        .maxOutputTokens(MAX_OUTPUT_TOKENS)
        .maxSteps(config.getMaxSteps())
        .store(Boolean.TRUE.equals(request.getStoreConversation()))
        .previousResponseId(request.getPrevResponseId())
        .addTool(new WebSearchTool(
            config.getSearchContextSize(),
            config.getMaxResults(),
            userLocation(request),
            new WebSearchFilters(denylist(properties.getJobsHunter().getBlacklist()), recency(config))))
        .instructions(templateRenderer.getPrompt(PromptType.SYSTEM_INSTRUCTIONS))
        .addSystemPrompt(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_JOB_SEARCH,
            "blacklist",
            properties.getJobsHunter().getBlacklist()
        ))
        .addUserPrompt(request.getUserPrompt())
        .setResponseSchema("job_search_results", templateRenderer.getSchema(AiSchemaType.PERPLEXITY_JSON_SCHEMA_RESPONSE))
        .build();

    return toJobsResponse(call(request, payload));
  }

  // ---------------------------------------------------------------------------
  // Search companies
  // ---------------------------------------------------------------------------

  @Override
  @Timed(value = "ai.api.search", extraTags = {"provider", "perplexity", "operation", "companies"})
  @CircuitBreaker(name = "perplexityCircuitBreaker", fallbackMethod = "fallbackSearchCompanies")
  @RateLimiter(name = "perplexityLimiter")
  @Bulkhead(name = "perplexityBulkhead")
  public List<CompanyDto> searchCompanies(PerplexitySearchRequest request) {
    return retryTemplate.execute(RetryPolicies.COMPANY_SEARCH, PROVIDER, () -> searchCompaniesOnce(request));
  }

  private List<CompanyDto> searchCompaniesOnce(PerplexitySearchRequest request) {
    Perplexity config = properties.getPerplexity();
    UserEntity user = request.getOrder().getUser();
    PerplexityJobsPayload payload = PerplexityJobsPayload.builder(request.getCompaniesModel())
        .store(false)
        .maxOutputTokens(MAX_OUTPUT_TOKENS)
        .maxSteps(config.getCompanyMaxSteps())
        .addTool(new WebSearchTool(config.getSearchContextSize(), config.getMaxResults(), userLocation(request), null))
        .addSystemPrompt(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_COMPANY_SEARCH,
            Map.of("city", user.getCity(),
                "country", user.getCountry()
            )))
        .addUserPrompt(templateRenderer.getPrompt(PromptType.USER_PROMPT_COMPANIES,
            Map.of(
                "domain", user.getJobDomain(),
                "city", user.getCity(),
                "country", user.getCountry()
            )))
        .setResponseSchema("companies_results", templateRenderer.getSchema(AiSchemaType.PERPLEXITY_JSON_COMPANY_SCHEMA_RESPONSE))
        .build();

    return extractCompanies(call(request, payload));
  }

  // ---------------------------------------------------------------------------
  // Search jobs of one company
  // ---------------------------------------------------------------------------

  @Override
  @Timed(value = "ai.api.search", extraTags = {"provider", "perplexity", "operation", "jobs-from-companies"})
  @CircuitBreaker(name = "perplexityCircuitBreaker", fallbackMethod = "fallbackSearch")
  @RateLimiter(name = "perplexityLimiter")
  @Bulkhead(name = "perplexityBulkhead")
  public AiClientResponse searchJobsFromCompanies(PerplexitySearchRequest request) {
    return retryTemplate.execute(RetryPolicies.JOB_SEARCH_BY_COMPANY, PROVIDER, () -> searchJobsByCompanyOnce(request));
  }

  private AiClientResponse searchJobsByCompanyOnce(PerplexitySearchRequest request) {
    Perplexity config = properties.getPerplexity();
    UserEntity user = request.getOrder().getUser();
    CompanyDto company = request.getCompany();
    List<String> positions = user.getJobRoles().stream().map(UserJobRoleEntity::getJobRole).toList();

    PerplexityJobsPayload payload = PerplexityJobsPayload.builder(request.getDiscoveryModel())
        .maxOutputTokens(MAX_OUTPUT_TOKENS)
        .maxSteps(config.getMaxSteps())
        .reasoning(new Reasoning(REASONING_JOB_SEARCH))
        .store(Boolean.TRUE.equals(request.getStoreConversation()))
        .previousResponseId(request.getPrevResponseId())
        .addTool(new WebSearchTool(
            config.getSearchContextSize(),
            config.getMaxResults(),
            userLocation(request),
            new WebSearchFilters(companyAllowlist(company.careersPage()), null)))
        .addTool(new FetchUrlTool(COMPANY_CAREERS_MAX_URLS))
        .instructions(templateRenderer.getPrompt(PromptType.SYSTEM_INSTRUCTIONS))
        .addSystemPrompt(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_JOBS_BY_COMPANY))
        .addUserPrompt(templateRenderer.getPrompt(PromptType.USER_PROMPT_JOB,
            Map.of(
                "company_name", company.companyName(),
                "careers_page", company.careersPage(),
                "positions", positions
            )
        ))
        .setResponseSchema("job_search_results", templateRenderer.getSchema(AiSchemaType.PERPLEXITY_JSON_SCHEMA_RESPONSE))
        .build();

    return toJobsResponse(call(request, payload));
  }

  // ---------------------------------------------------------------------------
  // HTTP call
  // ---------------------------------------------------------------------------

  /**
   * Sends the payload, publishes the cost (also for failed runs, they are billed) and rejects responses that carry no usable answer.
   */
  private PerplexityResponse call(PerplexitySearchRequest request, PerplexityJobsPayload payload) {
    TokenEstimationResult estmTokens = tokenEstimationGuard.assertFitsContext(payload);

    PerplexityResponse response = restClient.post()
        .uri(properties.getPerplexity().getBaseUrl())
        .headers(h -> h.setBearerAuth(properties.getPerplexity().getApiKey()))
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .body(PerplexityResponse.class);

    if (response == null) {
      throw new IllegalStateException("Empty response from Perplexity for model " + payload.model());
    }
    costPublisher.publishPerplexity(request.getOrder().getJobOrder().getId(), payload.aiModel(), estmTokens, response.usage());

    if (PerplexityResponse.STATUS_FAILED.equals(response.status())) {
      String reason = response.error() == null ? "unknown" : response.error().type() + ": " + response.error().message();
      throw new IllegalStateException("Perplexity response " + response.id() + " failed: " + reason);
    }
    if (PerplexityResponse.STATUS_INCOMPLETE.equals(response.status())) {
      log.warn("Perplexity response {} is incomplete (token limit reached), parsing what was returned", response.id());
    }
    return response;
  }

  private AiClientResponse toJobsResponse(PerplexityResponse response) {
    AiClientResponse result = new AiClientResponse();
    result.setId(response.id());
    result.addAll(urlVerifier.verify(extractJobs(response), response));
    return result;
  }

  // ---------------------------------------------------------------------------
  // Request helpers
  // ---------------------------------------------------------------------------

  private static UserLocation userLocation(PerplexitySearchRequest request) {
    UserEntity user = request.getOrder().getUser();
    //Country is the ISO code, like RO
    return new UserLocation(request.getCountryIsoCode(), StringUtils.removeDiacritics(user.getCity()), null);
  }

  private static String recency(Perplexity config) {
    return StringUtils.isBlank(config.getRecencyFilter()) ? null : config.getRecencyFilter();
  }

  /**
   * Converts the comma separated blacklist to a {@code search_domain_filter} denylist ({@code -domain}), truncated to the 20 entries the API
   * accepts. The rest of the blacklist is still enforced downstream by the validation rules.
   */
  static List<String> denylist(String blacklist) {
    if (StringUtils.isBlank(blacklist)) {
      return null;
    }
    List<String> domains = Arrays.stream(blacklist.split(","))
        .map(String::trim)
        .filter(d -> !d.isEmpty())
        .map(d -> "-" + d)
        .toList();
    if (domains.size() > WebSearchFilters.MAX_DOMAINS) {
      log.warn("Blacklist has {} domains, Perplexity accepts only {} in search_domain_filter, the rest is ignored",
          domains.size(), WebSearchFilters.MAX_DOMAINS);
      domains = domains.subList(0, WebSearchFilters.MAX_DOMAINS);
    }
    return domains.isEmpty() ? null : domains;
  }

  /**
   * Allowlist for the per-company search: the careers page host plus the usual ATS domains. Mixing allow and deny entries is not allowed by the
   * API, so no blacklist here.
   */
  static List<String> companyAllowlist(String careersPage) {
    Set<String> domains = new LinkedHashSet<>();
    String host = PerplexityUrlVerifier.host(careersPage);
    if (host != null) {
      domains.add(host);
    }
    domains.addAll(ATS_DOMAINS);
    return domains.stream().limit(WebSearchFilters.MAX_DOMAINS).toList();
  }

  // ---------------------------------------------------------------------------
  // Response parsing
  // ---------------------------------------------------------------------------

  protected List<Job> extractJobs(PerplexityResponse response) {
    List<Job> jobs = new ArrayList<>();
    for (String text : outputTexts(response)) {
      try {
        JobSearchResponse parsed = mapper.readValue(text, JobSearchResponse.class);
        parsed.results().stream()
            .map(JobResult::job_posting_url)
            .filter(url -> url != null && !url.isBlank())
            .map(Job::new)
            .forEach(jobs::add);
      } catch (Exception e) {
        log.warn("Perplexity response {} is not valid json ({}), extracting urls from text", response.id(), e.getMessage());
        jobs.addAll(urlExtractor.parseJobs(text));
      }
    }
    return jobs;
  }

  protected List<CompanyDto> extractCompanies(PerplexityResponse response) {
    List<CompanyDto> companies = new ArrayList<>();
    for (String text : outputTexts(response)) {
      try {
        companies.addAll(mapper.readValue(text, CompanyDtoList.class).results());
      } catch (JsonProcessingException e) {
        throw new IllegalStateException("Perplexity response " + response.id() + " is not a valid companies json", e);
      }
    }
    return companies;
  }

  /** Texts of the {@code output_text} contents of the assistant messages. */
  private static List<String> outputTexts(PerplexityResponse response) {
    if (response.output() == null) {
      return List.of();
    }
    return response.output().stream()
        .filter(item -> OutputItem.TYPE_MESSAGE.equals(item.type()) && item.content() != null)
        .flatMap(item -> item.content().stream())
        .filter(c -> ContentItem.TYPE_OUTPUT_TEXT.equals(c.type()))
        .map(ContentItem::text)
        .filter(Objects::nonNull)
        .filter(text -> text.length() > 2)
        .toList();
  }

  // ---------------------------------------------------------------------------
  // Resilience fallbacks (resolved by name by Resilience4j)
  // ---------------------------------------------------------------------------

  @SuppressWarnings("unused")
  private AiClientResponse fallbackSearch(PerplexitySearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded fallbackSearch: {}", getClass().getSimpleName(), t.getMessage(), t);
    return new AiClientResponse();
  }

  @SuppressWarnings("unused")
  private List<CompanyDto> fallbackSearchCompanies(PerplexitySearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded fallbackSearchCompanies: {}", getClass().getSimpleName(), t.getMessage(), t);
    return List.of();
  }
}
