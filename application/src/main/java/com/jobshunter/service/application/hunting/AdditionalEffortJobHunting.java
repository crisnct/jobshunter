package com.jobshunter.service.application.hunting;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.dto.AdditionalEffortRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.EngineSelection;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.PromptType;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.application.processors.JobsStateMachine;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.DeleteConvAiClient;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.annotation.Nonnull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class AdditionalEffortJobHunting<T extends AdditionalEffortRequest> extends GenericJobHunting<T> {

  private final TemplateRenderer templateRenderer;

  private final JobsStateMachine jobsStateMachine;

  private final int maxRetries;

  public AdditionalEffortJobHunting(Executor executor,
      AiJobsClient<T, AiClientResponse> jobsClient,
      UserCvService userCvService,
      TemplateRenderer templateRenderer,
      JobsStateMachine jobsStateMachine,
      ApplicationProperties applicationProperties
  ) {
    super(executor, jobsClient, userCvService);
    this.templateRenderer = templateRenderer;
    this.jobsStateMachine = jobsStateMachine;
    this.maxRetries = applicationProperties.getJobsHunter().getAdditionalEffort().getMaxRetries();
  }

  @Override
  protected CompletableFuture<AiClientResponse> searchAsync(T request, Executor executor) {
    return searchAsyncWithRetry(request, executor, 0, new AiClientResponse());
  }

  private CompletableFuture<AiClientResponse> searchAsyncWithRetry(T request, Executor executor, int retryCount,
      AiClientResponse accumulatedResponse) {
    //Firstly try with user prompt
    return CompletableFuture.supplyAsync(() -> searchSync(request), executor)
        .thenCompose(response -> {
          if (response.getJobs().isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
          }
          log.info("Conversation response id: {}", response.getId());
          request.setPrevResponseId(response.getId());
          //Pass jobs to state machine for processing to see which URLs are accepted/rejected
          CompletableFuture<List<Job>> jobsFuture = CompletableFuture.completedFuture(response.getJobs());
          return jobsStateMachine.processAsyncWithContext(jobsFuture, request.getUser(), false);
        })
        //Then, if some URLs are rejected, retry with modified prompt
        .thenCompose(contexts -> searchAsyncWithRetry(request, executor, retryCount, accumulatedResponse, contexts))
        .thenApplyAsync(aiClientResponse -> {
          deleteConversationSync(request);
          return aiClientResponse;
        },executor)
        .handle((response, ex) -> handleErrorsSync(request, accumulatedResponse, response, ex));
  }

  @Nonnull
  private CompletableFuture<AiClientResponse> searchAsyncWithRetry(
      T request,
      Executor executor,
      int retryCount,
      AiClientResponse accumulatedResponse,
      List<JobContext> contexts
  ) {
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

    // Stage 4: Retry if there are rejected jobs and retry limit not reached
    if (!rejectedJobs.isEmpty() && retryCount < maxRetries) {
      log.info("Found {} rejected jobs for user {}. Retrying search (attempt {}/{})",
          rejectedJobs.size(), request.getUser().getUsername(), retryCount + 1, maxRetries);

      String newPrompt = this.generateRejectedJobsPrompt(rejectedJobs);
      T retryRequest = this.createRetryRequest(request, accumulatedResponse, newPrompt);
      return this.searchAsyncWithRetry(retryRequest, executor, retryCount + 1, accumulatedResponse);
    } else {
      if (!rejectedJobs.isEmpty()) {
        log.warn("Max retries ({}) reached for user {}. {} jobs remain rejected.",
            maxRetries, request.getUser().getUsername(), rejectedJobs.size());
      }
      return CompletableFuture.completedFuture(accumulatedResponse);
    }
  }

  private void deleteConversationSync(T request) {
    if (jobsClient instanceof DeleteConvAiClient client && request.getPrevResponseId() != null) {
      client.deleteConversation(request.getPrevResponseId());
      log.info("Deleted conversation with id {}", request.getPrevResponseId());
    }
  }

  private String generateRejectedJobsPrompt(List<Job> rejectedJobs) {
    List<String> rejectedUrls = rejectedJobs.stream()
        .map(Job::getUrl)
        .toList();
    return templateRenderer.getPrompt(PromptType.USER_PROMPT_JOB_BLAME_1,
        "timestamp", Instant.now(),
        "invalid_urls", String.join(", ", rejectedUrls));
  }

  @SuppressWarnings("unchecked")
  private T createRetryRequest(T originalRequest, AiClientResponse prevResponse, String newPrompt) {
    try {
      // Try to create a new instance using the constructor that takes UserEntity and EngineSelection
      //TODO check if this is best practice
      T retryRequest = (T) originalRequest.clone();

      // Copy all fields from original request
      retryRequest.setPromptId(originalRequest.getPromptId());
      retryRequest.setUserPrompt(newPrompt);
      retryRequest.setSearchCompanies(originalRequest.isSearchCompanies());
      retryRequest.setStoreConversation(originalRequest.getStoreConversation());
      retryRequest.setFileId(null);
      retryRequest.setPrevResponseId(prevResponse.getId());
      retryRequest.setPreviousURL(originalRequest.getPreviousURL());
      return retryRequest;
    } catch (Exception e) {
      log.warn("Failed to create retry request copy, modifying original request instead", e);
      // Fallback: modify original request (safe in single-threaded CompletableFuture chain)
      originalRequest.setUserPrompt(newPrompt);
      return originalRequest;
    }
  }

  private AiClientResponse handleErrorsSync(T request, AiClientResponse accumulatedResponse, AiClientResponse response, Throwable ex) {
    if (ex != null) {
      EngineSelection engineConfig = request.getEngineSelection();
      if (ex.getCause() != null && ex.getCause() instanceof RequestNotPermitted) {
        log.error("❌ Rate limit exceeded for user {}, engine: {}, model: {}",
            request.getUser().getUsername(), engineConfig.type(), engineConfig.model());
      } else {
        log.error("Unexpected error at gathering jobs from model {}: {} for prompt {}", engineConfig.model(),
            ex.getMessage(), request.getUserPrompt());
      }
      log.error("Pipeline failed for searchAsyncWithRetry", ex);
      return accumulatedResponse.getJobs().isEmpty() ? new AiClientResponse() : accumulatedResponse;
    }
    if (response == null) {
      log.error("Pipeline searchAsyncWithRetry returned null response for user {}", request.getUser().getUsername());
      return accumulatedResponse.getJobs().isEmpty() ? new AiClientResponse() : accumulatedResponse;
    }
    return response;
  }

  private void chainConversation(T request, AiClientResponse response, String model) {
    request.setPreviousURL(response.getJobs().stream().map(Job::getUrl).toList());
    String prevId = response.getId();
    for (PromptType promptType : List.of(
        PromptType.USER_PROMPT_JOB_SERIES_1,
        PromptType.USER_PROMPT_JOB_SERIES_2,
        PromptType.USER_PROMPT_JOB_SERIES_3
    )) {
      if (promptType == PromptType.USER_PROMPT_JOB_SERIES_3 && request.getPreviousURL().size() < 2) {
        break;
      }
      //This is mandatory, otherwise the AI model will reply nothing in conversation.
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      AiClientResponse anotherResponse = jobsClient.searchJobs(request);
      log.info("Found {} jobs for {}", anotherResponse.getJobs().size(), request.getUser().getUsername());
      response.addAll(anotherResponse);

      //This is mandatory, otherwise the AI model will reply nothing in conversation.
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      Map<String, Object> params = switch (promptType) {
        case USER_PROMPT_JOB_SERIES_1 -> Map.of("timestamp", Instant.now());
        case USER_PROMPT_JOB_SERIES_2 -> Map.of("invalid_urls", request.getPreviousURL(), "invalid_reasons", RandomInvalidReasons.pick());
        case USER_PROMPT_JOB_SERIES_3 -> {
          int mid = request.getPreviousURL().size() / 2;
          List<String> firstHalf = request.getPreviousURL().subList(0, mid);
          List<String> secondHalf = request.getPreviousURL().subList(mid, request.getPreviousURL().size());
          yield Map.of("timestamp", Instant.now(), "valid_results_json", firstHalf, "invalid_urls", secondHalf);
        }
        default -> Map.of();
      };
      String prompt = templateRenderer.getPrompt(promptType, params);

      log.info("Searching jobs for user {} with model {} with prompt {}", request.getUser().getUsername(), model,
          StringUtils.abbreviate(prompt, 50));
      request.setUserPrompt(prompt);
      request.setPrevResponseId(prevId);
      AiClientResponse otherResponse = jobsClient.searchJobs(request);
      log.info("Found {} jobs for {}", otherResponse.getJobs().size(), request.getUser().getUsername());

      response.addAll(otherResponse);
      prevId = otherResponse.getId();
    }
  }

}
