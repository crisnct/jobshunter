package com.jobshunter.service.application.hunting.hunters;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.service.ModelsDBService;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.JobSearchRequest;
import com.jobshunter.dto.PerplexitySearchRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.EngineSelection;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.hunting.CountryIsoCode;
import com.jobshunter.service.application.hunting.JobByCompanyHunting;
import com.jobshunter.service.application.hunting.JobByPromptHunting;
import com.jobshunter.service.application.hunting.JobHunting;
import com.jobshunter.service.application.hunting.strategies.AiConversationStrategy;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.AiJobsCompaniesClient;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Job hunter backed by the Perplexity Agent API. Rejected jobs are fed back on the same conversation ({@code previous_response_id}) by
 * {@link AiConversationStrategy}, exactly like GPT and Grok. Perplexity has no endpoint to delete a conversation, so the cleanup step is a no-op.
 */
@Slf4j
@Service
public final class PerplexityJobHunting implements JobHunting, JobByPromptHunting, JobByCompanyHunting {

  private final AiJobsClient<PerplexitySearchRequest> jobsClient;
  private final Executor executor;
  private final AiConversationStrategy jobSearchStrategy;
  private final ModelsDBService modelsDBService;
  private final CountryIsoCode countryIsoCode;
  private final ApplicationProperties properties;

  private AiModelEntity discoveryModel;
  private AiModelEntity companiesModel;

  public PerplexityJobHunting(
      @Qualifier("perplexitySearchExecutor") Executor executor,
      @Qualifier("JobsClientPERPLEXITY") AiJobsClient<PerplexitySearchRequest> perplexityClient,
      ModelsDBService modelsDBService,
      AiConversationStrategy strategy,
      CountryIsoCode countryIsoCode,
      ApplicationProperties properties
  ) {
    this.executor = executor;
    this.jobsClient = perplexityClient;
    this.modelsDBService = modelsDBService;
    this.jobSearchStrategy = strategy;
    this.countryIsoCode = countryIsoCode;
    this.properties = properties;
  }

  @EventListener(ApplicationReadyEvent.class)
  private void init() {
    this.companiesModel = modelsDBService
        .getModel(new EngineSelection(EngineType.PERPLEXITY, properties.getPerplexity().getCompaniesModel()))
        .orElseThrow();
    this.discoveryModel = modelsDBService
        .getModel(new EngineSelection(EngineType.PERPLEXITY, properties.getPerplexity().getDiscoveryModel()))
        .orElseThrow();
  }

  @Override
  public EngineType getEngineType() {
    return EngineType.PERPLEXITY;
  }

  // ---------------------------------------------------------------------------
  // Request building
  // ---------------------------------------------------------------------------

  private PerplexitySearchRequest createBaseRequest(SearchJobOrder order) {
    return PerplexitySearchRequest.builder(order)
        .countryIsoCode(countryIsoCode.getCode(order.getUser().getCountry()))
        // store=false hides the response from GET, but it still works as previous_response_id for the retry round
        .storeConversation(false)
        .discoveryModel(discoveryModel)
        .companiesModel(companiesModel)
        .build();
  }

  private PerplexitySearchRequest createRequest(SearchJobOrder order, UserPromptEntity prompt) {
    return createBaseRequest(order).toBuilder()
        .userPrompt(prompt.getPrompt())
        .promptId(prompt.getId())
        .build();
  }

  // ---------------------------------------------------------------------------
  // Primary search orchestration
  // ---------------------------------------------------------------------------

  @Override
  public CompletableFuture<List<Job>> searchJobsAsync(SearchJobOrder order) {
    List<PerplexitySearchRequest> requests = order.getUser().getPrompts().stream()
        .map(prompt -> createRequest(order, prompt))
        .toList();

    if (requests.isEmpty()) {
      return CompletableFuture.completedFuture(List.of());
    }

    List<CompletableFuture<List<Job>>> futures = requests.stream()
        .map(request -> jobSearchStrategy.searchAsync(request, executor, this::searchSync, this::cleanupConversation)
            .thenApply(AiClientResponse::getJobs))
        .toList();

    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
        .thenApply(v -> futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .toList());
  }

  private AiClientResponse searchSync(JobSearchRequest request) {
    PerplexitySearchRequest perplexityRequest = (PerplexitySearchRequest) request;
    AiModelEntity aiModel = perplexityRequest.getOrder().getModel();
    UserEntity user = perplexityRequest.getOrder().getUser();
    log.info("Searching jobs for user {} with model {}", user.getUsername(), aiModel.getModel());
    AiClientResponse response = jobsClient.searchJobs(perplexityRequest);
    response.getJobs().forEach(job -> {
      job.setPromptId(perplexityRequest.getPromptId());
      job.setSource(aiModel.getModel());
    });
    log.info("{} found {} url's and are going to be validated", aiModel.getModel(), response.getJobs().size());
    return response;
  }

  /** Perplexity exposes no DELETE for responses, nothing to clean up. */
  private void cleanupConversation(JobSearchRequest request) {
    // intentionally empty
  }

  // ---------------------------------------------------------------------------
  // Company-based search
  // ---------------------------------------------------------------------------

  @Override
  public CompletableFuture<List<Job>> searchJobsByCompaniesAsync(SearchJobOrder order) {
    UserEntity user = order.getUser();
    PerplexitySearchRequest request = createBaseRequest(order);

    if (jobsClient instanceof AiJobsCompaniesClient<?> jobsClientComp) {
      @SuppressWarnings("unchecked")
      AiJobsCompaniesClient<PerplexitySearchRequest> typedClient = (AiJobsCompaniesClient<PerplexitySearchRequest>) jobsClientComp;
      return searchCompaniesAndJobsAsync(request, order.getModel(), typedClient)
          .exceptionally(throwable -> {
            if (throwable.getCause() instanceof RequestNotPermitted) {
              log.error("❌ Rate limit exceeded for user {} model {}", user.getUsername(), order.getModel());
            } else {
              Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
              log.error("Unexpected error at gathering jobs from model {}: {}", order.getModel(), cause.getMessage());
            }
            return List.of();
          });
    } else {
      log.warn("No implemented company search for {}", order.getModel().getModel());
      return CompletableFuture.completedFuture(List.of());
    }
  }

  private CompletableFuture<List<Job>> searchCompaniesAndJobsAsync(
      PerplexitySearchRequest request,
      AiModelEntity model,
      AiJobsCompaniesClient<PerplexitySearchRequest> client
  ) {
    CompletableFuture<List<CompanyDto>> companiesFuture = CompletableFuture.supplyAsync(() -> {
      log.info("Searching companies for user {} with model {}", request.getOrder().getUser().getUsername(), model.getModel());
      List<CompanyDto> companyDtos = client.searchCompanies(request);
      log.info("Found {} companies for user {} with model {}, searching jobs in parallel...",
          companyDtos.size(), request.getOrder().getUser().getUsername(), model.getModel());
      return companyDtos;
    }, executor);

    return companiesFuture.thenCompose(companies -> {
      if (companies.isEmpty()) {
        return CompletableFuture.completedFuture(List.of());
      } else {
        return searchJobsFromCompanyAsync(request, model, client, companies);
      }
    });
  }

  @Nonnull
  private CompletableFuture<List<Job>> searchJobsFromCompanyAsync(
      PerplexitySearchRequest request,
      AiModelEntity model,
      AiJobsCompaniesClient<PerplexitySearchRequest> client,
      List<CompanyDto> companies
  ) {
    List<CompletableFuture<List<Job>>> jobFutures = companies.stream()
        .map(company -> {
          PerplexitySearchRequest companyRequest = request.toBuilder().company(company).build();
          return jobSearchStrategy.searchAsync(
                  companyRequest, executor,
                  jobSearchRequest -> searchJobsFromCompanySync((PerplexitySearchRequest) jobSearchRequest, client),
                  this::cleanupConversation)
              .thenApply(AiClientResponse::getJobs);
        })
        .toList();

    return CompletableFuture.allOf(jobFutures.toArray(CompletableFuture[]::new))
        .thenApply(v -> jobFutures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .peek(job -> job.setSource("COMP-" + model.getModel()))
            .toList()
        );
  }

  private AiClientResponse searchJobsFromCompanySync(
      PerplexitySearchRequest companyRequest,
      AiJobsCompaniesClient<PerplexitySearchRequest> client
  ) {
    String username = companyRequest.getOrder().getUser().getUsername();
    String companyName = companyRequest.getCompany().companyName();
    log.info("Searching jobs for user {} from company: {} with model {}", username, companyName, companyRequest.getOrder().getModel());
    AiClientResponse jobs = client.searchJobsFromCompanies(companyRequest);
    log.info("Found {} jobs for user {} from company {}", jobs.getJobs().size(), username, companyName);
    return jobs;
  }

}
