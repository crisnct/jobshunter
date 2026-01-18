package com.jobshunter.service.clients.grok;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobRoleEntity;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.CompanyDtoList;
import com.jobshunter.dto.IpInfoDetailResponse;
import com.jobshunter.dto.exceptions.BusinessException;
import com.jobshunter.dto.grokRequest.GrokJobsPayload;
import com.jobshunter.dto.grokRequest.GrokJobsPayload.GrokJobsPayloadBuilder;
import com.jobshunter.dto.grokRequest.Reasoning;
import com.jobshunter.dto.grokRequest.tools.Tools;
import com.jobshunter.dto.grokRequest.tools.UserLocation;
import com.jobshunter.dto.grokResponse.GrokResponse;
import com.jobshunter.dto.grokResponse.OutputItem;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.AiSchemaType;
import com.jobshunter.model.GrokJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.model.PromptType;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.UrlExtractor;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.DeleteConvAiClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.jsonwebtoken.lang.Collections;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("JobsClientGROK")
@PackageExpected("com.jobshunter.service.clients.grok")
@ConditionalOnProperty(name = "grok.enabled", havingValue = "true")
@AllArgsConstructor
public non-sealed class GrokV1JobSearchImpl implements
    AiJobsClient<GrokJobSearchRequest, AiClientResponse>,
    DeleteConvAiClient {

  public static final URI DEFAULT_URI = URI.create("https://api.x.ai/v1/responses");
  private static final double DEFAULT_TEMPERATURE = 0.2;
  private final ApplicationProperties properties;

  private final RestClient restClient;

  private final JsonMapper mapper;

  private final UrlExtractor urlExtractor;

  private final TemplateRenderer templateRenderer;

  @Override
  @CircuitBreaker(name = "grokCircuitBreaker", fallbackMethod = "fallbackSearch")
  @RateLimiter(name = "grokLimiter")
  @Bulkhead(name = "grokBulkhead")
  public AiClientResponse searchJobs(GrokJobSearchRequest request) {
    try {
      GrokJobsPayloadBuilder payloadBuilder = GrokJobsPayload.builder(request.getModel())
          .temperature(DEFAULT_TEMPERATURE)
          .reasoning(new Reasoning(REASONING_JOB_SEARCH))
          .store(request.getStoreConversation())
          .previousResponseId(request.getPrevResponseId())
          .addTools(Tools.builder().setWebSearch().build())
          .maxOutputTokens(7000)
          .addUserPrompt(request.getUserPrompt() + templateRenderer.getPrompt(PromptType.USER_PROMPT_JOB_BLACKLISTED,
              "blacklist",
              properties.getJobsHunter().getBlacklist()
          ), request.getFileId());

      GrokJobsPayload payload = payloadBuilder.build();
      GrokResponse response = restClient.post()
          .uri(DEFAULT_URI)
          .headers((h) -> h.setBearerAuth(properties.getGrok().getApiKey()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(GrokResponse.class);

      //noinspection DataFlowIssue
      List<Job> jobs = extractJobs(response);
      AiClientResponse result = new AiClientResponse();
      result.setId(response.id());
      result.addAll(jobs);
      return result;
    } catch (Exception e) {
      log.error("GROK API call failed", e);
      return new AiClientResponse();
    }
  }

  @Override
  @CircuitBreaker(name = "grokCircuitBreaker", fallbackMethod = "fallbackSearchCompanies")
  @RateLimiter(name = "grokLimiter")
  @Bulkhead(name = "grokBulkhead")
  public List<CompanyDto> searchCompanies(GrokJobSearchRequest request) {
    try {
      UserEntity user = request.getUser();

      IpInfoDetailResponse ipInfo = request.getIpInfo();

      UserLocation userLocation = new UserLocation();
      userLocation.setType("approximate");
      //This is country iso code, like RO
      userLocation.setCountry(ipInfo.country() != null ? ipInfo.country() : "RO");
      userLocation.setCity(request.getUser().getCity() != null ? request.getUser().getCity() : ipInfo.city());

      GrokJobsPayload payload = GrokJobsPayload.builder(request.getModel())
          .temperature(DEFAULT_TEMPERATURE)
          .store(false)
          .reasoning(new Reasoning(REASONING_COMPANY_SEARCH))
          .maxOutputTokens(2500)
          .addTools(Tools.builder().setWebSearch().userLocation(userLocation).build())
          .addSystemPrompt(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_COMPANY_SEARCH,
              Map.of("city", user.getCity(),
                  "country", user.getCountry(),
                  "timestamp", String.valueOf(Instant.now())
              )))
          .addUserPrompt(templateRenderer.getPrompt(PromptType.USER_PROMPT_COMPANIES,
              Map.of(
                  "domain", user.getJobDomain(),
                  "city", user.getCity(),
                  "country", user.getCountry(),
                  "positions", user.getJobRoles(),
                  "blacklist", properties.getJobsHunter().getBlacklist()
              )), request.getFileId())
          .setResponseSchema(templateRenderer.getSchema(AiSchemaType.GROK_JSON_COMPANY_SCHEMA_RESPONSE))
          .build();

      GrokResponse response = restClient.post()
          .uri(DEFAULT_URI)
          .headers((h) -> h.setBearerAuth(properties.getGrok().getApiKey()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(GrokResponse.class);

      //noinspection DataFlowIssue
      return extractCompanies(response);
    } catch (Exception e) {
      log.error("GROK job API call failed", e);
      return List.of();
    }
  }

  @Override
  @CircuitBreaker(name = "grokCircuitBreaker", fallbackMethod = "fallbackSearchJobsFromCompanies")
  @RateLimiter(name = "grokLimiter")
  @Bulkhead(name = "grokBulkhead")
  public AiClientResponse searchJobsFromCompanies(GrokJobSearchRequest request, List<CompanyDto> group) {
    String userPrompt = templateRenderer.getPrompt(PromptType.USER_PROMPT_JOB,
        "positions", request.getUser().getJobRoles().stream().map(UserJobRoleEntity::getJobRole).toList().toString(),
        "companies", StringUtils.join(group.stream().map(CompanyDto::companyName).toList())
    ) + templateRenderer.getPrompt(PromptType.USER_PROMPT_JOB_BLACKLISTED,
        "blacklist",
        properties.getJobsHunter().getBlacklist()
    );

    GrokJobsPayload payload = GrokJobsPayload.builder(request.getModel())
        .temperature(DEFAULT_TEMPERATURE)
        .maxOutputTokens(7000)
        .reasoning(new Reasoning(REASONING_JOB_SEARCH))
        .store(request.getStoreConversation())
        .previousResponseId(request.getPrevResponseId())
        .addTools(Tools.builder().setWebSearch().build())
        .addUserPrompt(userPrompt, request.getFileId())
        .build();

    GrokResponse response = restClient.post()
        .uri(DEFAULT_URI)
        .headers((h) -> h.setBearerAuth(properties.getGrok().getApiKey()))
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .body(GrokResponse.class);
    //noinspection DataFlowIssue
    List<Job> jobs = extractJobs(response);
    AiClientResponse result = new AiClientResponse();
    result.setId(response.id());
    result.addAll(jobs);
    return result;
  }

  @Override
  @CircuitBreaker(name = "grokCircuitBreaker", fallbackMethod = "fallbackDeleteConversation")
  @RateLimiter(name = "grokLimiter")
  @Bulkhead(name = "grokBulkhead")
  public void deleteConversation(String id) {
    restClient.delete()
        .uri(DEFAULT_URI + "/" + id)
        .headers((h) -> h.setBearerAuth(properties.getGrok().getApiKey()))
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
  private void fallbackDeleteConversation(String id, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
  }

  @SuppressWarnings("unused")
  private AiClientResponse fallbackSearchJobsFromCompanies(GrokJobSearchRequest request, List<CompanyDto> group, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return new AiClientResponse();
  }

  @SuppressWarnings("unused")
  private AiClientResponse fallbackSearch(GrokJobSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return new AiClientResponse();
  }

  @SuppressWarnings("unused")
  private List<CompanyDto> fallbackSearchCompanies(GrokJobSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return List.of();
  }

  protected List<Job> extractJobs(GrokResponse response) {
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
          .forEach(o -> jobs.addAll(urlExtractor.parseJobs(o.text())));
      return jobs;
    } else {
      return java.util.Collections.emptyList();
    }
  }

  protected List<CompanyDto> extractCompanies(GrokResponse response) {
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
