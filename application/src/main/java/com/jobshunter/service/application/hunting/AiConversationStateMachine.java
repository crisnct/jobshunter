package com.jobshunter.service.application.hunting;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.dto.AdditionalEffortRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.EngineSelection;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobMetadataType;
import com.jobshunter.service.application.processors.JobsStateMachine;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
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
        .thenCompose(response -> startConversation(request, accumulatedResponse, response))
        .thenCompose(contexts -> processWithRetry(request, executor, retryCount, accumulatedResponse, contexts,
            searchExecutor, promptGenerator, retryRequestFactory, conversationCleanup))
        .whenCompleteAsync((_, _) -> {
          try {
            conversationCleanup.cleanup(request);
          } catch (Exception cleanupEx) {
            log.warn("Failed to cleanup conversation for user {}: {}", request.getUser().getUsername(), cleanupEx.getMessage());
          }
        }, executor)
        .exceptionally(ex -> handlePipelineError(request, accumulatedResponse, ex));
  }

  private <R extends AdditionalEffortRequest> CompletableFuture<List<JobContext>> startConversation(
      R request,
      AiClientResponse accumulatedResponse,
      AiClientResponse response
  ) {
    log.info("Conversation response id: {}", response.getId());
    request.setPrevResponseId(response.getId());
    List<Job> jobsFound = response.getJobs()
        .stream()
        .filter(p -> !accumulatedResponse.contains(p.getUrl()))
        .toList();
    if (jobsFound.isEmpty()) {
      return CompletableFuture.completedFuture(List.of());
    } else {
      // Process Phase: Pass jobs to state machine for processing
      return jobsStateMachine.processAsync(CompletableFuture.completedFuture(jobsFound), request.getUser());
    }
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
      ConversationCleanup<R> conversationCleanup
  ) {
    // Filter Phase: Separate accepted and rejected jobs
    contexts.stream()
        .filter(ctx -> !ctx.isFailed() && ctx.isValidatedSuccessfully())
        .map(JobContext::getJob)
        .forEach(job -> {
          job.addMetadata(JobMetadataType.APPROVED_BY_CONVERSATION_STATE_MACHINE, true);
          accumulatedResponse.add(job);
        });

    List<Job> rejectedJobs = contexts.stream()
        .filter(ctx -> !ctx.isFailed() && !ctx.isValidatedSuccessfully())
        .map(JobContext::getJob)
        .toList();

    // Retry Phase: Retry if there are rejected jobs and retry limit not reached
    if (!rejectedJobs.isEmpty() && retryCount < maxRetries) {
      log.info("Found {} rejected jobs for user {}. Retrying search (attempt {}/{})",
          rejectedJobs.size(), request.getUser().getUsername(), retryCount + 1, maxRetries);

      String newPrompt = promptGenerator.generate(rejectedJobs);
      R retryRequest = retryRequestFactory.create(request, accumulatedResponse, newPrompt);
      return processAsyncWithRetry(retryRequest, executor, retryCount + 1, accumulatedResponse,
          searchExecutor, promptGenerator, retryRequestFactory, conversationCleanup);
    } else {
      if (!rejectedJobs.isEmpty()) {
        log.warn("Max retries ({}) reached for user {}. {} jobs remain rejected.",
            maxRetries, request.getUser().getUsername(), rejectedJobs.size());
      }
      return CompletableFuture.completedFuture(accumulatedResponse);
    }
  }

  private <R extends AdditionalEffortRequest> AiClientResponse handlePipelineError(
      R request,
      AiClientResponse accumulatedResponse,
      Throwable ex
  ) {
    EngineSelection engineConfig = request.getEngineSelection();
    if (ex.getCause() instanceof RequestNotPermitted) {
      log.error("Rate limit exceeded for user {}, engine: {}, model: {}",
          request.getUser().getUsername(), engineConfig.type(), engineConfig.model());
    } else {
      log.error("Unexpected error at gathering jobs from model {}: {} for prompt {}",
          engineConfig.model(), ex.getMessage(), request.getUserPrompt());
    }
    log.error("Pipeline failed for searchAsync", ex);
    return accumulatedResponse.getJobs().isEmpty() ? new AiClientResponse() : accumulatedResponse;
  }

}
