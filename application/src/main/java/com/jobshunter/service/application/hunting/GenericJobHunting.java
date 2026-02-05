package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.entities.UserRemoteCvEntity;
import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.AiJobsCompaniesClient;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract non-sealed class GenericJobHunting implements JobHunting {

  protected final AiJobsClient jobsClient;
  protected final UserCvService userCvService;
  private final Executor executor;
  private final CountryIsoCode countryIsoCode;

  public GenericJobHunting(
      Executor executor,
      AiJobsClient jobsClient,
      CountryIsoCode countryIsoCode,
      UserCvService userCvService
  ) {
    this.executor = executor;
    this.countryIsoCode = countryIsoCode;
    this.jobsClient = jobsClient;
    this.userCvService = userCvService;
  }

  public abstract EngineType getEngineType();

  public AIJobSearchRequest createRequest(SearchJobOrder order) {
    String fileId = order.getUser().getRemoteCvs().stream()
        .filter(p -> p.getProvider() == getEngineType())
        .findAny()
        .map(UserRemoteCvEntity::getFileId)
        .orElse(null);

    String countryCode = countryIsoCode != null
        ? countryIsoCode.getCode(order.getUser().getCountry())
        : null;

    return AIJobSearchRequest.builder(order)
        .fileId(fileId)
        .base64CV(Base64.getEncoder().encodeToString(order.getUser().getCv().getByteArray()))
        .storeConversation(true)
        .countryIsoCode(countryCode)
        .build();
  }

  public AIJobSearchRequest createRequest(SearchJobOrder order, String prompt, Long promptId) {
    AIJobSearchRequest base = this.createRequest(order);
    return base.toBuilder()
        .userPrompt(prompt)
        .promptId(promptId)
        .build();
  }

  public AIJobSearchRequest createRequest(@NotNull SearchJobOrder order, @NotNull UserPromptEntity prompt) {
    return createRequest(order, prompt.getPrompt(), prompt.getId());
  }

  @Override
  public CompletableFuture<List<Job>> searchJobsAsync(SearchJobOrder order) {
    final List<CompletableFuture<List<Job>>> futures;

    if (order.getModel().getProvider() == EngineType.SERP) {
      // Search jobs for each role in parallel
      futures = order.getUser().getJobRoles().stream()
          .map(role -> createRequest(order, role.getJobRole(), null))
          .map(request -> searchAsync(request, executor)
              .thenApply(AiClientResponse::getJobs))
          .toList();
    } else {
      // Search jobs based on user prompts in parallel
      futures = order.getUser().getPrompts().stream()
          .map(prompt -> createRequest(order, prompt))
          .map(request -> searchAsync(request, executor)
              .thenApply(AiClientResponse::getJobs))
          .toList();
    }

    if (futures.isEmpty()) {
      return CompletableFuture.completedFuture(List.of());
    }

    // Wait for all searches to complete and merge results
    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
        .thenApply(v -> futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .toList());
  }

  public CompletableFuture<List<Job>> searchJobsByCompaniesAsync(SearchJobOrder order) {
    UserEntity user = order.getUser();
    AIJobSearchRequest request = createRequest(order);

    if (jobsClient instanceof AiJobsCompaniesClient jobsClientComp) {
      return searchCompaniesAndJobsAsync(request, order.getModel(), jobsClientComp)
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

  protected CompletableFuture<AiClientResponse> searchAsync(AIJobSearchRequest request, Executor executor) {
    return CompletableFuture.supplyAsync(() -> searchSync(request), executor)
        .orTimeout(30, TimeUnit.MINUTES)
        .exceptionally(throwable -> {
          AiModelEntity aiModel = request.getOrder().getModel();
          Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;

          if (cause instanceof TimeoutException) {
            log.warn("⏱️ Timeout exceeded for user {}, engine: {}, model: {}, prompt: {}",
                request.getOrder().getUser().getUsername(),
                aiModel.getProvider(), aiModel.getModel(),
                request.getUserPrompt());
          } else if (cause instanceof RequestNotPermitted) {
            log.error("❌ Rate limit exceeded for user {}, engine: {}, model: {}",
                request.getOrder().getUser().getUsername(), aiModel.getProvider(), aiModel.getModel());
          } else {
            log.error("Unexpected error at gathering jobs from model {}: {} for prompt {}", aiModel.getModel(),
                throwable.getMessage(), request.getUserPrompt());
          }
          return new AiClientResponse();
        });
  }

  protected AiClientResponse searchSync(AIJobSearchRequest request) {
    AiModelEntity aiModel = request.getOrder().getModel();
    UserEntity user = request.getOrder().getUser();
    log.info("Searching jobs for user {} with model {}", user.getUsername(), aiModel.getModel());
    AiClientResponse response = jobsClient.searchJobs(request);
    response.getJobs().forEach(job -> {
      job.setPromptId(request.getPromptId());
      job.setSource(aiModel.getModel());
    });
    log.info("{} found {} url's and are going to be validated", aiModel.getModel(), response.getJobs().size());
    return response;
  }

  private CompletableFuture<List<Job>> searchCompaniesAndJobsAsync(
      AIJobSearchRequest request,
      AiModelEntity model,
      AiJobsCompaniesClient client
  ) {
    // Step 1: Search for companies asynchronously
    CompletableFuture<List<CompanyDto>> companiesFuture = CompletableFuture.supplyAsync(() -> {
      log.info("Searching companies for user {} with model {}", request.getOrder().getUser().getUsername(), model.getModel());
      List<CompanyDto> companyDtos = client.searchCompanies(request);
      log.info("Found {} companies for user {} with model {}, searching jobs in parallel...",
          companyDtos.size(), request.getOrder().getUser().getUsername(), model.getModel());
      return companyDtos;
    }, executor);

    // Step 2: For each company, search jobs in parallel
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
      AIJobSearchRequest request,
      AiModelEntity model,
      AiJobsCompaniesClient client,
      List<CompanyDto> companies
  ) {
    // Create a future for each company's job search
    List<CompletableFuture<List<Job>>> jobFutures = companies.stream()
        .map(company -> {
          // Create a copy of the request with the company set (thread-safe via toBuilder)
          AIJobSearchRequest companyRequest = request.toBuilder()
              .company(company)
              .build();
          String username = request.getOrder().getUser().getUsername();

          return CompletableFuture.supplyAsync(() -> {
            log.info("Searching jobs for user {} from company: {} with model {}", username, company.companyName(), model.getModel());
            List<Job> jobs = client.searchJobsFromCompanies(companyRequest).getJobs();
            log.info("Found {} jobs for user {} from company {}", jobs.size(), username, company.companyName());
            return jobs;
          }, executor);
        })
        .toList();

    // Combine all futures and merge results
    return CompletableFuture.allOf(jobFutures.toArray(CompletableFuture[]::new))
        .thenApply(v -> jobFutures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .peek(job -> job.setSource("COMP-" + model.getModel()))
            .toList()
        );
  }

}
