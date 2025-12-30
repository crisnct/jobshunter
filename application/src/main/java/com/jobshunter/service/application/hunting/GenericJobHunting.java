package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.EngineConfigurationEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.clients.AiJobsClient;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract non-sealed class GenericJobHunting<T extends AIJobSearchRequest> implements JobHunting {

  private final Executor geminiSearchExecutor;

  private final AiJobsClient<T, List<Job>> jobsClient;

  public GenericJobHunting(
      Executor executor,
      AiJobsClient<T, List<Job>> jobsClient
  ) {
    this.geminiSearchExecutor = executor;
    this.jobsClient = jobsClient;
  }

  public abstract T createRequest(UserEntity user, UserPromptEntity prompt);

  @Override
  public CompletableFuture<List<Job>> searchJobsAsync(SearchJobOrder order) {
    UserEntity user = order.user();

    CompletableFuture<List<Job>> futures = CompletableFuture.completedFuture(List.of());
    List<UserPromptEntity> prompts = user.getPrompts().stream().filter(p -> contains(order, p)).toList();

    for (UserPromptEntity prompt : prompts) {
      T request = createRequest(user, prompt);

      CompletableFuture<List<Job>> jobsFound = this.searchAsync(request, geminiSearchExecutor);
      futures = futures.thenCombine(jobsFound, (previousJobs, newJobs) -> {
        List<Job> merged = new ArrayList<>(previousJobs);
        merged.addAll(newJobs);
        return merged;
      });
    }
    return futures;
  }

  private CompletableFuture<List<Job>> searchAsync(T request, Executor executor) {
    return CompletableFuture.supplyAsync(() -> searchSync(request), executor)
        .exceptionally(throwable -> {
          EngineConfigurationEntity engineConfig = request.getPrompt().getEngineConfiguration();
          if (throwable.getCause() != null && throwable.getCause() instanceof RequestNotPermitted) {
            log.error("❌ Rate limit exceed for user {}, Engine: {}, Model: {}",
                request.getUsername(), engineConfig.getEngine(), engineConfig.getModel());
          } else {
            log.error("Unexpected error at gathering jobs from model {}: {} for prompt {}", engineConfig.getModel(),
                throwable.getMessage(), request.getPrompt().getPrompt());
          }
          return List.of();
        });
  }

  @Nonnull
  private List<Job> searchSync(T request) {
    EngineConfigurationEntity engineConfig = request.getPrompt().getEngineConfiguration();
    log.info("Searching jobs for user {} with model {}", request.getUsername(), engineConfig.getModel());
    List<Job> jobsFound = jobsClient.searchJobs(request);
    jobsFound.forEach(job -> {
      job.setPromptId(request.getPrompt().getId());
      job.setSource(engineConfig.getModel());
    });
    String model = engineConfig.getModel();
    log.info("{} found {} url's and are going to be validated", model, jobsFound.size());
    return jobsFound;
  }

  private boolean contains(SearchJobOrder order, UserPromptEntity prompt) {
    return order.engines().stream()
        .anyMatch(p -> p.type() == prompt.getEngineConfiguration().getEngine());
  }
}
