package com.jobshunter.service.clients.gpt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobRoleEntity;
import com.jobshunter.database.entities.UserRemoteCvEntity;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.CompanyDtoList;
import com.jobshunter.dto.exceptions.ValidationException;
import com.jobshunter.dto.gptRequest.GptJobsPayload;
import com.jobshunter.dto.gptRequest.tools.Tools;
import com.jobshunter.dto.gptResponse.GptResponse;
import com.jobshunter.dto.gptResponse.OutputItem;
import com.jobshunter.model.AiSchemaType;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.GptJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.model.PromptType;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.security.JHHeaders;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.UrlExtractor;
import com.jobshunter.service.clients.AiJobsClient;
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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("JobsClientGPT")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "true")
@AllArgsConstructor
public non-sealed class GptV1JobSearchImpl implements AiJobsClient<GptJobSearchRequest, List<Job>> {

  public static final URI DEFAULT_URI = URI.create("https://api.openai.com/v1/responses");

  private final ApplicationProperties properties;

  private final RestClient restClient;

  private final JsonMapper mapper;

  private final UrlExtractor urlExtractor;

  private final TemplateRenderer templateRenderer;

  @Override
  @CircuitBreaker(name = "gptCircuitBreaker", fallbackMethod = "fallbackSearch")
  @RateLimiter(name = "gptLimiter")
  @Bulkhead(name = "gptBulkhead")
  public List<Job> searchJobs(GptJobSearchRequest request) {
    try {
      UserRemoteCvEntity remoteCV = request.getUser().getRemoteCvs().stream()
          .filter(p-> p.getProvider() == EngineType.GPT).findAny()
          .orElseThrow(()-> new ValidationException("No GPT CV found for user " + request.getUser().getId()));

      GptJobsPayload payload = GptJobsPayload.builder()
          .model(request.getOrder().getEngineSelection().model())
          .reasoning(request.getReasoning())
          .store(false)
          .max_output_tokens(properties.getGpt().getMaxTokens())
          .addTools(Tools.builder().setDeepSearch().build())
          .addSystemPrompt(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_JOB_SEARCH))
          .addUserPrompt(request.getPrompt().getPrompt(), remoteCV.getFileId())
          .build();

      GptResponse response = restClient.post()
          .uri(DEFAULT_URI)
          .header(JHHeaders.AUTHORIZATION, "Bearer " + properties.getGpt().getApiKey())
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(GptResponse.class);

      //noinspection DataFlowIssue
      return extractJobs(response);
    } catch (Exception e) {
      log.error("GPT API call failed", e);
      return List.of();
    }
  }

  @Override
  @CircuitBreaker(name = "gptCircuitBreaker", fallbackMethod = "fallbackSearchCompanies")
  @RateLimiter(name = "gptLimiter")
  @Bulkhead(name = "gptBulkhead")
  public List<CompanyDto> searchCompanies(GptJobSearchRequest request) {
    try {
      UserEntity user = request.getUser();
      GptJobsPayload payload = GptJobsPayload.builder()
          .model(request.getOrder().getEngineSelection().model())
          .max_output_tokens(properties.getGpt().getMaxTokens())
          .store(false)
          .addSystemPrompt(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_COMPANY_SEARCH,
              "city", user.getCity(),
              "country", user.getCountry(),
              "timestamp", String.valueOf(Instant.now())
          ))
          .addUserPrompt(templateRenderer.getPrompt(PromptType.USER_PROMPT_COMPANIES,
              Map.of(
                  "domain", user.getJobDomain(),
                  "city", user.getCity(),
                  "country", user.getCountry(),
                  "positions", user.getJobRoles()
              )))
          .setResponseSchema(templateRenderer.getSchema(AiSchemaType.GPT_JSON_COMPANY_SCHEMA_RESPONSE))
          .build();

      GptResponse response = restClient.post()
          .uri(DEFAULT_URI)
          .header(JHHeaders.AUTHORIZATION, "Bearer " + properties.getGpt().getApiKey())
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(GptResponse.class);

      //noinspection DataFlowIssue
      return extractCompanies(response);
    } catch (Exception e) {
      log.error("GPT job API call failed", e);
      return List.of();
    }
  }

  @Override
  @CircuitBreaker(name = "gptCircuitBreaker", fallbackMethod = "fallbackSearchJobsFromCompanies")
  @RateLimiter(name = "gptLimiter")
  @Bulkhead(name = "gptBulkhead")
  public List<Job> searchJobsFromCompanies(GptJobSearchRequest request, List<CompanyDto> group) {
    String userPrompt = templateRenderer.getPrompt(PromptType.USER_PROMPT_JOB,
        "positions", request.getUser().getJobRoles().stream().map(UserJobRoleEntity::getJobRole).toList().toString(),
        "companies", StringUtils.join(group.stream().map(CompanyDto::companyName).toList())
    );
    GptJobsPayload payload = GptJobsPayload.builder()
        .model(request.getOrder().getEngineSelection().model())
        .max_output_tokens(properties.getGpt().getMaxTokens())
        .store(false)
        .reasoning(request.getReasoning())
        .addTools(Tools.builder().setDeepSearch().build())
        .addSystemPrompt(templateRenderer.getPrompt(PromptType.SYSTEM_PROMPT_JOB_SEARCH))
        .addUserPrompt(userPrompt)
        .build();

    GptResponse response = restClient.post()
        .uri(DEFAULT_URI)
        .header(JHHeaders.AUTHORIZATION, "Bearer " + properties.getGpt().getApiKey())
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .body(GptResponse.class);
    //noinspection DataFlowIssue
    return extractJobs(response);
  }

  @SuppressWarnings("unused")
  private List<Job> fallbackSearchJobsFromCompanies(GptJobSearchRequest request, List<CompanyDto> group, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return List.of();
  }

  @SuppressWarnings("unused")
  private List<Job> fallbackSearch(GptJobSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return List.of();
  }

  @SuppressWarnings("unused")
  private List<CompanyDto> fallbackSearchCompanies(GptJobSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
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
          .forEach(o -> jobs.addAll(urlExtractor.parseJobs(o.text())));
      return jobs;
    } else {
      return java.util.Collections.emptyList();
    }
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
