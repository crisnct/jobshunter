package com.jobshunter.service.application.processors;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.dto.AdditionalEffortRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiConversationStateMachine {

  @FunctionalInterface
  public interface SearchExecutor<R extends AdditionalEffortRequest> {
    AiClientResponse execute(R request);
  }

  @FunctionalInterface
  public interface PromptGenerator {
    String generate(List<Job> rejectedJobs);
  }

  @FunctionalInterface
  public interface RetryRequestFactory<R extends AdditionalEffortRequest> {
    R create(R originalRequest, AiClientResponse prevResponse, String newPrompt);
  }

  @FunctionalInterface
  public interface ConversationCleanup<R extends AdditionalEffortRequest> {
    void cleanup(R request);
  }

  @FunctionalInterface
  public interface ErrorHandler<R extends AdditionalEffortRequest> {
    AiClientResponse handle(R request, AiClientResponse accumulatedResponse,
        AiClientResponse response, Throwable ex);
  }

  private final JobsStateMachine jobsStateMachine;
  private final int maxRetries;

  public AiConversationStateMachine(
      JobsStateMachine jobsStateMachine,
      ApplicationProperties applicationProperties
  ) {
    this.jobsStateMachine = jobsStateMachine;
    this.maxRetries = applicationProperties.getJobsHunter().getAdditionalEffort().getMaxRetries();
  }

  public <R extends AdditionalEffortRequest> CompletableFuture<AiClientResponse> processAsync(
      R request,
      Executor executor,
      SearchExecutor<R> searchExecutor,
      PromptGenerator promptGenerator,
      RetryRequestFactory<R> retryRequestFactory,
      ConversationCleanup<R> conversationCleanup,
      ErrorHandler<R> errorHandler
  ) {
    return processAsyncWithRetry(request, executor, 0, new AiClientResponse(),
        searchExecutor, promptGenerator, retryRequestFactory, conversationCleanup, errorHandler);
  }

  private <R extends AdditionalEffortRequest> CompletableFuture<AiClientResponse> processAsyncWithRetry(
      R request,
      Executor executor,
      int retryCount,
      AiClientResponse accumulatedResponse,
      SearchExecutor<R> searchExecutor,
      PromptGenerator promptGenerator,
      RetryRequestFactory<R> retryRequestFactory,
      ConversationCleanup<R> conversationCleanup,
      ErrorHandler<R> errorHandler
  ) {
    // Search Phase: Execute initial search
    return CompletableFuture.supplyAsync(() -> searchExecutor.execute(request), executor)
        .thenCompose(response -> {
          if (response.getJobs().isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
          }
          log.info("Conversation response id: {}", response.getId());
          request.setPrevResponseId(response.getId());
          // Process Phase: Pass jobs to state machine for processing
          CompletableFuture<List<Job>> jobsFuture = CompletableFuture.completedFuture(response.getJobs());
          return jobsStateMachine.processAsyncWithContext(jobsFuture, request.getUser(), false);
        })
        // Filter and Retry Phase: Handle accepted/rejected jobs
        .thenCompose(contexts -> processWithRetry(request, executor, retryCount, accumulatedResponse, contexts,
            searchExecutor, promptGenerator, retryRequestFactory, conversationCleanup, errorHandler))
        // Cleanup Phase: Delete conversation after completion
        .thenApplyAsync(aiClientResponse -> {
          conversationCleanup.cleanup(request);
          return aiClientResponse;
        }, executor)
        .handle((response, ex) -> errorHandler.handle(request, accumulatedResponse, response, ex));
  }

  @Nonnull
  private <R extends AdditionalEffortRequest> CompletableFuture<AiClientResponse> processWithRetry(
      R request,
      Executor executor,
      int retryCount,
      AiClientResponse accumulatedResponse,
      List<JobContext> contexts,
      SearchExecutor<R> searchExecutor,
      PromptGenerator promptGenerator,
      RetryRequestFactory<R> retryRequestFactory,
      ConversationCleanup<R> conversationCleanup,
      ErrorHandler<R> errorHandler
  ) {
    // Filter Phase: Separate accepted and rejected jobs
    List<Job> acceptedJobs = contexts.stream()
        .filter(ctx -> !ctx.isFailed() && ctx.isAccepted())
        .map(JobContext::getJob)
        .toList();

    List<Job> rejectedJobs = contexts.stream()
        .filter(ctx -> !ctx.isFailed() && !ctx.isAccepted())
        .map(JobContext::getJob)
        .toList();

    // Add accepted jobs to accumulated response
    accumulatedResponse.addAll(acceptedJobs);

    // Retry Phase: Retry if there are rejected jobs and retry limit not reached
    if (!rejectedJobs.isEmpty() && retryCount < maxRetries) {
      log.info("Found {} rejected jobs for user {}. Retrying search (attempt {}/{})",
          rejectedJobs.size(), request.getUser().getUsername(), retryCount + 1, maxRetries);

      String newPrompt = promptGenerator.generate(rejectedJobs);
      R retryRequest = retryRequestFactory.create(request, accumulatedResponse, newPrompt);
      return processAsyncWithRetry(retryRequest, executor, retryCount + 1, accumulatedResponse,
          searchExecutor, promptGenerator, retryRequestFactory, conversationCleanup, errorHandler);
    } else {
      if (!rejectedJobs.isEmpty()) {
        log.warn("Max retries ({}) reached for user {}. {} jobs remain rejected.",
            maxRetries, request.getUser().getUsername(), rejectedJobs.size());
      }
      return CompletableFuture.completedFuture(accumulatedResponse);
    }
  }

}
