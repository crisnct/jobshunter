package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.EngineCategory;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.clients.AiJobsClient;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Gatherers;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract non-sealed class GenericJobHunting<T extends AIJobSearchRequest> implements JobHunting {

  protected final AiJobsClient<T, AiClientResponse> jobsClient;
  protected final UserCvService userCvService;
  private final Executor executor;

  public GenericJobHunting(
      Executor executor,
      AiJobsClient<T, AiClientResponse> jobsClient,
      UserCvService userCvService
  ) {
    this.executor = executor;
    this.jobsClient = jobsClient;
    this.userCvService = userCvService;
  }

  public abstract T createRequest(SearchJobOrder order, UserPromptEntity prompt);

  public abstract T createCompaniesRequest(SearchJobOrder order);

  @Override
  public CompletableFuture<List<Job>> searchJobsAsync(SearchJobOrder order) {
    //Search companies and then for each company search jobs
    CompletableFuture<List<Job>> futures = CompletableFuture.completedFuture(List.of());
    //Search jobs based on user requests
    boolean isAImodel = Arrays.stream(EngineType.values()).anyMatch(p -> p == order.getModel().getProvider());
    for (UserPromptEntity prompt : order.getUser().getPrompts()) {
      if (((prompt.getEngineCategory() == EngineCategory.AI) && isAImodel)
          || ((prompt.getEngineCategory() != EngineCategory.AI) && !isAImodel)) {

        T request = createRequest(order, prompt);
        CompletableFuture<AiClientResponse> jobsFound = this.searchAsync(request, executor);
        futures = futures.thenCombine(jobsFound, (previousJobs, newJobs) -> {
          List<Job> merged = new ArrayList<>(previousJobs);
          merged.addAll(newJobs.getJobs());
          return merged;
        });
      }
    }

    return futures;
  }

  public CompletableFuture<List<Job>> searchJobsByCompaniesAsync(SearchJobOrder order) {
    UserEntity user = order.getUser();
    T request = createCompaniesRequest(order);
    //TODO optimize and create an async for each of the job search for a group of companies.
    // Do it similar like in searchJobsAsync so it will run all jobs search in parallel
    return CompletableFuture.supplyAsync(() -> searchCompaniesSync(request, order.getModel()), executor)
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
  }

  protected CompletableFuture<AiClientResponse> searchAsync(T request, Executor executor) {
    return CompletableFuture.supplyAsync(() -> searchSync(request), executor)
        .exceptionally(throwable -> {
          AiModelEntity aiModel = request.getOrder().getModel();
          if (throwable.getCause() != null && throwable.getCause() instanceof RequestNotPermitted) {
            log.error("❌ Rate limit exceeded for user {}, engine: {}, eodel: {}",
                request.getOrder().getUser().getUsername(), aiModel.getProvider(), aiModel.getModel());
          } else {
            log.error("Unexpected error at gathering jobs from model {}: {} for prompt {}", aiModel.getModel(),
                throwable.getMessage(), request.getUserPrompt());
          }
          return new AiClientResponse();
        });
  }

  protected AiClientResponse searchSync(T request) {
    AiModelEntity aiModel = request.getOrder().getModel();
    UserEntity user = request.getOrder().getUser();
    log.info("Searching jobs for user {} with model {}", user.getUsername(), aiModel.getModel());
    if (user.getCv() != null) {
      //Upload cv if needed
      userCvService.refreshUserCvIfNeeded(user, aiModel.getProvider());
    }
    AiClientResponse response = jobsClient.searchJobs(request);
    response.getJobs().forEach(job -> {
      job.setPromptId(request.getPromptId());
      job.setSource(aiModel.getModel());
    });

    log.info("{} found {} url's and are going to be validated", aiModel.getModel(), response.getJobs().size());
    return response;
  }

  private List<Job> searchCompaniesSync(T request, AiModelEntity model) {
    UserEntity user = request.getOrder().getUser();
    log.info("Searching companies for user {} with model {}", user.getUsername(), model.getModel());
    List<CompanyDto> companies = jobsClient.searchCompanies(request);
    log.info("Found {} companies for user {} with model {}, searching jobs now...", companies.size(), user.getUsername(),
        model.getModel());
    List<List<CompanyDto>> companiesGrouped = companies.stream()
        .gather(Gatherers.windowFixed(5))
        .toList();

    List<Job> jobsFound = new ArrayList<>();
    for (List<CompanyDto> group : companiesGrouped) {
      log.info("Searching jobs for user {} from companies: {}", user.getUsername(),
          String.join(", ", group.stream().map(CompanyDto::companyName).toList()));
      List<Job> jobs = jobsClient.searchJobsFromCompanies(request, group).getJobs();
      jobsFound.addAll(jobs);
      log.info("Found {} jobs for user {} from current group of companies.", jobs.size(), user.getUsername());
    }
    jobsFound.forEach(job -> job.setSource("COMP-" + model.getModel()));
    return jobsFound;
  }

}
