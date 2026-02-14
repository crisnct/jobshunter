package com.jobshunter.service.application.hunting.hunters;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.entities.UserRemoteCvEntity;
import com.jobshunter.database.service.ModelsDBService;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.GrokSearchRequest;
import com.jobshunter.dto.JobSearchRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.EngineSelection;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.hunting.JobByCompanyHunting;
import com.jobshunter.service.application.hunting.JobByPromptHunting;
import com.jobshunter.service.application.hunting.JobHunting;
import com.jobshunter.service.application.hunting.strategies.AiConversationStrategy;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.AiJobsCompaniesClient;
import com.jobshunter.service.clients.DeleteConvAiClient;
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

@Slf4j
@Service
public final class GrokJobHunting implements JobHunting, JobByPromptHunting, JobByCompanyHunting {

  private final AiJobsClient<GrokSearchRequest> jobsClient;
  private final Executor executor;
  private final AiConversationStrategy jobSearchStrategy;
  private final ModelsDBService modelsDBService;
  private AiModelEntity discoveryModel;
  private AiModelEntity companiesModel;

  public GrokJobHunting(
      @Qualifier("grokSearchExecutor") Executor grokSearchExecutor,
      @Qualifier("JobsClientGROK") AiJobsClient<GrokSearchRequest> grokClient,
      ModelsDBService modelsDBService,
      AiConversationStrategy strategy
  ) {
    this.executor = grokSearchExecutor;
    this.jobsClient = grokClient;
    this.modelsDBService = modelsDBService;
    this.jobSearchStrategy = strategy;
  }

  @EventListener(ApplicationReadyEvent.class)
  private void init() {
    this.companiesModel = modelsDBService.getModel(new EngineSelection(EngineType.GROK, "grok-4-fast-reasoning"))
        .orElseThrow();
    this.discoveryModel = modelsDBService.getModel(new EngineSelection(EngineType.GROK, "grok-4-fast-reasoning"))
        .orElseThrow();
  }

  @Override
  public EngineType getEngineType() {
    return EngineType.GROK;
  }

  // ---------------------------------------------------------------------------
  // Request building
  // ---------------------------------------------------------------------------

  private GrokSearchRequest createBaseRequest(SearchJobOrder order) {
    String fileId = order.getUser().getRemoteCvs().stream()
        .filter(p -> p.getProvider() == getEngineType())
        .findAny()
        .map(UserRemoteCvEntity::getFileId)
        .orElse(null);

    return GrokSearchRequest.builder(order)
        .fileId(fileId)
        .storeConversation(true)
        .discoveryModel(discoveryModel)
        .companiesModel(companiesModel)
        .build();
  }

  private GrokSearchRequest createRequest(SearchJobOrder order, String prompt, Long promptId) {
    GrokSearchRequest base = createBaseRequest(order);
    return base.toBuilder()
        .userPrompt(prompt)
        .promptId(promptId)
        .build();
  }

  private GrokSearchRequest createRequest(SearchJobOrder order, UserPromptEntity prompt) {
    return createRequest(order, prompt.getPrompt(), prompt.getId());
  }

  // ---------------------------------------------------------------------------
  // Primary search orchestration
  // ---------------------------------------------------------------------------

  @Override
  public CompletableFuture<List<Job>> searchJobsAsync(SearchJobOrder order) {
    List<GrokSearchRequest> requests = order.getUser().getPrompts().stream()
        .map(prompt -> createRequest(order, prompt))
        .toList();

    if (requests.isEmpty()) {
      return CompletableFuture.completedFuture(List.of());
    }

    List<CompletableFuture<List<Job>>> futures = requests.stream()
        .map(request -> jobSearchStrategy.searchAsync(
                request, executor, this::searchSync, this::cleanupConversation)
            .thenApply(AiClientResponse::getJobs))
        .toList();

    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
        .thenApply(v -> futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .toList());
  }

  // ---------------------------------------------------------------------------
  // Synchronous search
  // ---------------------------------------------------------------------------

  private AiClientResponse searchSync(JobSearchRequest request) {
    GrokSearchRequest grokRequest = (GrokSearchRequest) request;
    AiModelEntity aiModel = grokRequest.getOrder().getModel();
    UserEntity user = grokRequest.getOrder().getUser();
    log.info("Searching jobs for user {} with model {}", user.getUsername(), aiModel.getModel());
    AiClientResponse response = jobsClient.searchJobs(grokRequest);
    response.getJobs().forEach(job -> {
      job.setPromptId(grokRequest.getPromptId());
      job.setSource(aiModel.getModel());
    });
    log.info("{} found {} url's and are going to be validated", aiModel.getModel(), response.getJobs().size());
    return response;
  }

  // ---------------------------------------------------------------------------
  // Conversation cleanup
  // ---------------------------------------------------------------------------

  private void cleanupConversation(JobSearchRequest request) {
    if (request instanceof GrokSearchRequest grokRequest
        && jobsClient instanceof DeleteConvAiClient client
        && grokRequest.getPrevResponseId() != null) {
      client.deleteConversation(grokRequest.getPrevResponseId());
      log.info("Deleted conversation with id {}", grokRequest.getPrevResponseId());
    }
  }

  // ---------------------------------------------------------------------------
  // Company-based search
  // ---------------------------------------------------------------------------

  @Override
  public CompletableFuture<List<Job>> searchJobsByCompaniesAsync(SearchJobOrder order) {
    UserEntity user = order.getUser();
    GrokSearchRequest request = createBaseRequest(order);

    if (jobsClient instanceof AiJobsCompaniesClient<?> jobsClientComp) {
      @SuppressWarnings("unchecked")
      AiJobsCompaniesClient<GrokSearchRequest> typedClient = (AiJobsCompaniesClient<GrokSearchRequest>) jobsClientComp;
      return searchCompaniesAndJobsAsync(request, order.getModel(), typedClient)
          .exceptionally(throwable -> {
            if (throwable.getCause() != null && throwable.getCause() instanceof RequestNotPermitted) {
              log.error("❌ Rate limit exceeded for user {} model {}", user.getUsername(), order.getModel());
            } else {
              if (throwable.getCause() != null) {
                log.error("Unexpected error at gathering jobs from model {}: {}", order.getModel(),
                    throwable.getCause().getMessage());
              } else {
                log.error("Unexpected error at gathering jobs from model {}: {}", order.getModel(), throwable.getMessage());
              }
            }
            return List.of();
          });
    } else {
      log.warn("No implemented company search for {}", order.getModel().getModel());
      return CompletableFuture.completedFuture(List.of());
    }
  }

  private CompletableFuture<List<Job>> searchCompaniesAndJobsAsync(
      GrokSearchRequest request,
      AiModelEntity model,
      AiJobsCompaniesClient<GrokSearchRequest> client
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
      GrokSearchRequest request,
      AiModelEntity model,
      AiJobsCompaniesClient<GrokSearchRequest> client,
      List<CompanyDto> companies
  ) {
    List<CompletableFuture<List<Job>>> jobFutures = companies.stream()
        .map(company -> {
          GrokSearchRequest companyRequest = request.toBuilder()
              .company(company)
              .build();
          return jobSearchStrategy.searchAsync(
                  companyRequest, executor, jobSearchRequest -> searchJobsFromCompanySync((GrokSearchRequest) jobSearchRequest, client),
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
      GrokSearchRequest companyRequest,
      AiJobsCompaniesClient<GrokSearchRequest> client
  ) {
    String username = companyRequest.getOrder().getUser().getUsername();
    String companyName = companyRequest.getCompany().companyName();
    log.info("Searching jobs for user {} from company: {} with model {}", username, companyName, companyRequest.getOrder().getModel());
    AiClientResponse jobs = client.searchJobsFromCompanies(companyRequest);
    log.info("Found {} jobs for user {} from company {}", jobs.getJobs().size(), username, companyName);
    return jobs;
  }

}
