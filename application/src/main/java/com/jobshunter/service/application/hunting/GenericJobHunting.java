package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.model.EngineCategory;
import com.jobshunter.model.EngineSelection;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.clients.AiJobsClient;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Gatherers;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract non-sealed class GenericJobHunting<T extends AIJobSearchRequest> implements JobHunting {

  private final Executor executor;

  private final AiJobsClient<T, List<Job>> jobsClient;

  public GenericJobHunting(
      Executor executor,
      AiJobsClient<T, List<Job>> jobsClient
  ) {
    this.executor = executor;
    this.jobsClient = jobsClient;
  }

  public abstract T createRequest(SearchJobOrder order, UserPromptEntity prompt);

  public abstract T createCompaniesRequest(SearchJobOrder order);

  @Override
  public CompletableFuture<List<Job>> searchJobsAsync(SearchJobOrder order) {
    //Search companies and then for each company search jobs
    CompletableFuture<List<Job>> futures = CompletableFuture.completedFuture(List.of());
    //Search jobs based on user requests
    boolean isAImodel = Arrays.stream(EngineType.values()).anyMatch(p -> p == order.getEngineSelection().type());
    for (UserPromptEntity prompt : order.getUser().getPrompts()) {
      if (((prompt.getEngineCategory() == EngineCategory.AI) && isAImodel)
          || ((prompt.getEngineCategory() != EngineCategory.AI) && !isAImodel)) {

        T request = createRequest(order, prompt);
        CompletableFuture<List<Job>> jobsFound = this.searchAsync(request, executor);
        futures = futures.thenCombine(jobsFound, (previousJobs, newJobs) -> {
          List<Job> merged = new ArrayList<>(previousJobs);
          merged.addAll(newJobs);
          return merged;
        });
      }
    }

    return futures;
  }

  public CompletableFuture<List<Job>> searchJobsByCompaniesAsync(SearchJobOrder order) {
    UserEntity user = order.getUser();
    T request = createCompaniesRequest(order);
    return CompletableFuture.supplyAsync(() -> searchCompaniesSync(request, order.getEngineSelection()), executor)
        .exceptionally(throwable -> {
          if (throwable.getCause() != null && throwable.getCause() instanceof RequestNotPermitted) {
            log.error("❌ Rate limit exceeded for user {} model {}", user.getUsername(), order.getEngineSelection().model());
          } else {
            if (throwable.getCause() != null) {
              log.error("Unexpected error at gathering jobs from model {}: {}", order.getEngineSelection().model(),
                  throwable.getCause().getMessage());
            } else {
              log.error("Unexpected error at gathering jobs from model {}: {}", order.getEngineSelection().model(), throwable.getMessage());
            }
          }
          return List.of();
        });
  }

  private CompletableFuture<List<Job>> searchAsync(T request, Executor executor) {
    return CompletableFuture.supplyAsync(() -> searchSync(request), executor)
        .exceptionally(throwable -> {
          EngineSelection engineConfig = request.getOrder().getEngineSelection();
          if (throwable.getCause() != null && throwable.getCause() instanceof RequestNotPermitted) {
            log.error("❌ Rate limit exceeded for user {}, engine: {}, eodel: {}",
                request.getOrder().getUser().getUsername(), engineConfig.type(), engineConfig.model());
          } else {
            log.error("Unexpected error at gathering jobs from model {}: {} for prompt {}", engineConfig.model(),
                throwable.getMessage(), request.getPrompt().getPrompt());
          }
          return List.of();
        });
  }

  @Nonnull
  private List<Job> searchSync(T request) {
    String model = request.getOrder().getEngineSelection().model();
    log.info("Searching jobs for user {} with model {}", request.getUser().getUsername(), model);
    List<Job> jobsFound = jobsClient.searchJobs(request);
    jobsFound.forEach(job -> {
      job.setPromptId(request.getPrompt().getId());
      job.setSource(model);
    });

    log.info("{} found {} url's and are going to be validated", model, jobsFound.size());
    return jobsFound;
  }

  private List<Job> searchCompaniesSync(T request, EngineSelection engineSelection) {
    log.info("Searching companies for user {} with model {}", request.getUser().getUsername(), engineSelection.model());
    List<CompanyDto> companies = jobsClient.searchCompanies(request);
    log.info("Found {} companies for user {} with model {}, searching jobs now...", companies.size(), request.getUser().getUsername(),
        engineSelection.model());
    List<List<CompanyDto>> companiesGrouped = companies.stream()
        .gather(Gatherers.windowFixed(5))
        .toList();

    List<Job> jobsFound = new ArrayList<>();
    for (List<CompanyDto> group : companiesGrouped) {
      log.info("Searching jobs for user {} from companies: {}", request.getUser().getUsername(),
          String.join(", ", group.stream().map(CompanyDto::companyName).toList()));
      jobsFound.addAll(jobsClient.searchJobsFromCompanies(request, group));
      log.info("Found {} jobs for user {} from current group of companies.", jobsFound.size(), request.getUser().getUsername());
    }
    jobsFound.forEach(job -> job.setSource("COMP-" + engineSelection.model()));
    return jobsFound;
  }

}
