package com.jobshunter.service.application.hunting;

import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.Job;
import com.jobshunter.model.PromptType;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.DeleteConvAiClient;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AiConversationJobHunting extends GenericJobHunting {

  private final TemplateRenderer templateRenderer;

  private final AiConversationStateMachine conversationStateMachine;

  public AiConversationJobHunting(Executor executor,
      AiJobsClient jobsClient,
      UserCvService userCvService,
      TemplateRenderer templateRenderer,
      AiConversationStateMachine conversationStateMachine,
      CountryIsoCode countryIsoCode
  ) {
    super(executor, jobsClient, countryIsoCode, userCvService);
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
    return originalRequest.toBuilder()
        .userPrompt(newPrompt)
        .fileId(null)
        .prevResponseId(prevResponse.getId())
        .build();
  }

}
