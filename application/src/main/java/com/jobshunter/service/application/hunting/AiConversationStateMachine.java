package com.jobshunter.service.application.hunting;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.dto.AdditionalEffortRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobMetadataType;
import com.jobshunter.service.application.processors.JobsStateMachine;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiConversationStateMachine {

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
      ConversationCleanup<R> conversationCleanup
  ) {
    return processAsyncWithRetry(request, executor, 0, new AiClientResponse(),
        searchExecutor, promptGenerator, retryRequestFactory, conversationCleanup);
  }

  private <R extends AdditionalEffortRequest> CompletableFuture<AiClientResponse> processAsyncWithRetry(
      R request,
      Executor executor,
      int retryCount,
      AiClientResponse accumulatedResponse,
      SearchExecutor<R> searchExecutor,
      PromptGenerator promptGenerator,
      RetryRequestFactory<R> retryRequestFactory,
      ConversationCleanup<R> conversationCleanup
  ) {
    return CompletableFuture.supplyAsync(() -> searchExecutor.searchJobsSync(request), executor)
        .thenCompose(response -> removeDuplicates(request, response))
        .thenCompose(response -> validateJobsUrl(request, accumulatedResponse, response))
        .thenCompose(contexts -> collectValidJobs(accumulatedResponse, contexts))
        .thenCompose(contexts -> askAIForRejectedURLs(request, executor, retryCount, accumulatedResponse, contexts,
            searchExecutor, promptGenerator, retryRequestFactory, conversationCleanup))
        .whenComplete((_, _) -> CompletableFuture.runAsync(() -> {
          try {
            conversationCleanup.cleanup(request);
          } catch (Exception cleanupEx) {
            log.warn("Failed to cleanup conversation for user {}: {}", request.getOrder().getUser().getUsername(), cleanupEx.getMessage());
          }
        }))
        .exceptionally(ex -> handlePipelineError(request, accumulatedResponse, ex));
  }

  private <R extends AdditionalEffortRequest> CompletableFuture<AiClientResponse> removeDuplicates(
      R request,
      AiClientResponse response) {
    Set<String> seenUrls = new HashSet<>(request.getOrder().getIgnoredURLs());
    AiClientResponse aiClientResponse = new AiClientResponse();
    aiClientResponse.setId(response.getId());
    aiClientResponse.addAll(response.getJobs().stream()
        .filter(jc -> {
          String url = jc.getUrl();
          return url != null && seenUrls.add(url);
        })
        .toList());
    return CompletableFuture.completedFuture(aiClientResponse);
  }

  private <R extends AdditionalEffortRequest> CompletableFuture<List<JobContext>> validateJobsUrl(
      R request,
      AiClientResponse accumulatedResponse,
      AiClientResponse response
  ) {
    log.info("Conversation response id: {}", response.getId());
    request.setPrevResponseId(response.getId());
    List<Job> jobsFound = response.getJobs()
        .stream()
        .filter(p -> !accumulatedResponse.getJobs().contains(p))
        .toList();
    if (jobsFound.isEmpty()) {
      return CompletableFuture.completedFuture(List.of());
    } else {
      // Process Phase: Pass jobs to state machine for processing
      return jobsStateMachine.processAsync(CompletableFuture.completedFuture(jobsFound), request.getOrder().getUser());
    }
  }

  @Nonnull
  private <R extends AdditionalEffortRequest> CompletableFuture<AiClientResponse> askAIForRejectedURLs(
      R request,
      Executor executor,
      int retryCount,
      AiClientResponse accumulatedResponse,
      List<JobContext> contexts,
      SearchExecutor<R> searchExecutor,
      PromptGenerator promptGenerator,
      RetryRequestFactory<R> retryRequestFactory,
      ConversationCleanup<R> conversationCleanup
  ) {
    List<Job> rejectedJobs = contexts.stream()
        .filter(ctx -> !ctx.isFailed() && !ctx.isValidatedSuccessfully())
        .map(JobContext::getJob)
        .toList();

    // Retry Phase: Retry if there are rejected jobs and retry limit not reached
    if (!rejectedJobs.isEmpty() && retryCount < maxRetries) {
      log.info("Found {} rejected jobs for user {}. Retrying search (attempt {}/{})",
          rejectedJobs.size(), request.getOrder().getUser().getUsername(), retryCount + 1, maxRetries);

      String newPrompt = promptGenerator.generate(rejectedJobs);
      R retryRequest = retryRequestFactory.create(request, accumulatedResponse, newPrompt);
      return processAsyncWithRetry(retryRequest, executor, retryCount + 1, accumulatedResponse,
          searchExecutor, promptGenerator, retryRequestFactory, conversationCleanup);
    } else {
      if (!rejectedJobs.isEmpty()) {
        log.warn("Max retries ({}) reached for user {}. {} jobs remain rejected.",
            maxRetries, request.getOrder().getUser().getUsername(), rejectedJobs.size());
      }
      return CompletableFuture.completedFuture(accumulatedResponse);
    }
  }

  private CompletableFuture<List<JobContext>> collectValidJobs(
      AiClientResponse accumulatedResponse,
      List<JobContext> contexts
  ) {
    // Filter Phase: Separate accepted and rejected jobs
    contexts.stream()
        .filter(ctx -> !ctx.isFailed() && ctx.isValidatedSuccessfully())
        .map(JobContext::getJob)
        .forEach(job -> {
          job.addMetadata(JobMetadataType.APPROVED_BY_CONVERSATION_STATE_MACHINE, true);
          accumulatedResponse.add(job);
        });
    return CompletableFuture.completedFuture(contexts);
  }

  private <R extends AdditionalEffortRequest> AiClientResponse handlePipelineError(
      R request,
      AiClientResponse accumulatedResponse,
      Throwable ex
  ) {
    Throwable cause = unwrap(ex);
    AiModelEntity model = request.getOrder().getModel();
    if (cause instanceof RequestNotPermitted) {
      log.error("Rate limit exceeded for user {}, engine: {}, model: {}",
          request.getOrder().getUser().getUsername(), model.getProvider(), model.getModel());
    } else {
      log.error("Unexpected error at gathering jobs from model {}: {} for prompt {}",
          model.getModel(), cause != null ? cause.getMessage() : ex.getMessage(), request.getUserPrompt());
    }
    log.error("Pipeline failed for searchAsync", ex);
    return accumulatedResponse.getJobs().isEmpty() ? new AiClientResponse() : accumulatedResponse;
  }

  private Throwable unwrap(Throwable ex) {
    Throwable current = ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
    while (current != null && current.getCause() != null && !(current instanceof RequestNotPermitted)) {
      current = current.getCause();
    }
    return current;
  }

  @FunctionalInterface
  public interface SearchExecutor<R extends AdditionalEffortRequest> {

    AiClientResponse searchJobsSync(R request);
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

}
