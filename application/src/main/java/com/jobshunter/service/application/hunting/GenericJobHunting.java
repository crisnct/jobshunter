package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.JobsSynchronizer;
import com.jobshunter.service.clients.AiJobsClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
public abstract non-sealed class GenericJobHunting<T extends AIJobSearchRequest> implements JobHunting {

  private final Executor geminiSearchExecutor;

  private final AiJobsClient<T, List<Job>> economyModel;

  private final AiJobsClient<T, List<Job>> premiumModel;

  public GenericJobHunting(
      Executor executor,
      AiJobsClient<T, List<Job>> economyModel,
      AiJobsClient<T, List<Job>> premiumModel
  ) {
    this.geminiSearchExecutor = executor;
    this.economyModel = economyModel;
    this.premiumModel = premiumModel;
  }

  public abstract T createRequest(UserEntity user, UserPromptEntity prompt);

  public abstract long getDelayTaskExecution();

  @Override
  public CompletableFuture<Void> searchJobs(JobsSynchronizer jobsSync, SearchJobOrder order) {
    UserEntity user = order.user();
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    int delayCounter = 0;
    List<UserPromptEntity> prompts = user.getPrompts().stream().filter(p -> contains(order, p)).toList();
    for (UserPromptEntity prompt : prompts) {
      Executor delayedExecutor = CompletableFuture.delayedExecutor(
          (long) delayCounter++ * getDelayTaskExecution(),
          TimeUnit.MILLISECONDS,
          geminiSearchExecutor
      );
      T request = createRequest(user, prompt);
      futures.add(CompletableFuture.runAsync(() -> search(jobsSync, request), delayedExecutor));
    }

    // Combine all async iteration futures into one
    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
        .exceptionally(ex -> {
          log.error("{} search failed for {}", getClass().getSimpleName(), user.getUsername(), ex);
          return null;
        });
  }

  private void search(JobsSynchronizer jobsSync, T request) {
    log.info("Searching jobs for user {} with model {}-{}", request.getUsername(), request.getEngineType(), request.getEngineTier());
    List<Job> jobsFound = switch (request.getEngineTier()) {
      case ECONOMY -> economyModel.searchJobs(request);
      case PREMIUM -> premiumModel.searchJobs(request);
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid engine");
    };
    jobsSync.addJobs(jobsFound, request.getEngineType(), request.getEngineTier(), request.getPrompt().getId());
  }

  private boolean contains(SearchJobOrder order, UserPromptEntity prompt) {
    return order.engines().stream()
        .anyMatch(p -> p.type() == prompt.getEngineConfiguration().getEngineType() &&
            p.tier() == prompt.getEngineConfiguration().getTier());
  }
}
