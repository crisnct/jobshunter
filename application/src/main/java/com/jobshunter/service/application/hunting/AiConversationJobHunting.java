package com.jobshunter.service.application.hunting;

import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.Job;
import com.jobshunter.model.PromptType;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.DeleteConvAiClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class AiConversationJobHunting extends GenericJobHunting {

  private final TemplateRenderer templateRenderer;

  private final AiConversationStateMachine conversationStateMachine;

  public AiConversationJobHunting(Executor executor,
      AiJobsClient jobsClient,
      UserCvService userCvService,
      TemplateRenderer templateRenderer,
      AiConversationStateMachine conversationStateMachine
  ) {
    super(executor, jobsClient, userCvService);
    this.templateRenderer = templateRenderer;
    this.conversationStateMachine = conversationStateMachine;
  }

  @Override
  protected CompletableFuture<AiClientResponse> searchAsync(AIJobSearchRequest request, Executor executor) {
    return conversationStateMachine.processAsync(
        request,
        executor,
        this::searchSync,
        this::generateRejectedJobsPrompt,
        this::createRetryRequest,
        this::deleteConversationSync
    );
  }

  private void deleteConversationSync(AIJobSearchRequest request) {
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
        "invalid_urls", rejectedUrls);
  }

  private AIJobSearchRequest createRetryRequest(AIJobSearchRequest originalRequest, AiClientResponse prevResponse, String newPrompt) {
    // Create a copy using Copyable interface
    AIJobSearchRequest retryRequest = originalRequest.copy();

    // Override specific fields for retry
    retryRequest.setUserPrompt(newPrompt);
    retryRequest.setFileId(null);
    retryRequest.setPrevResponseId(prevResponse.getId());
    return retryRequest;
  }

  private void chainConversation(AIJobSearchRequest request, AiClientResponse response, String model) {
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
      SearchJobOrder order = request.getOrder();

      log.info("Found {} jobs for {}", anotherResponse.getJobs().size(), order.getUser().getUsername());
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

      log.info("Searching jobs for user {} with model {} with prompt {}", order.getUser().getUsername(), model,
          StringUtils.abbreviate(prompt, 50));
      //request.setPrompt(prompt);
      request.setPrevResponseId(prevId);
      AiClientResponse otherResponse = jobsClient.searchJobs(request);
      log.info("Found {} jobs for {}", otherResponse.getJobs().size(), order.getUser().getUsername());

      response.addAll(otherResponse);
      prevId = otherResponse.getId();
    }
  }

}
