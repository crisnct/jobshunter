package com.jobshunter.service.application.hunting.strategies;

import com.jobshunter.dto.JobSearchRequest;
import com.jobshunter.dto.JobSearchRequest.ConversationBuilder;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.Job;
import com.jobshunter.model.PromptType;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.hunting.AiConversationStateMachine;
import com.jobshunter.service.application.hunting.JobSearchStrategy;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Conversation-based search strategy with retry logic.
 * <p>
 * Delegates to the {@link AiConversationStateMachine} which validates returned
 * job URLs, asks the AI to replace rejected ones, and retries up to a
 * configurable maximum. Conversation cleanup is invoked after the pipeline
 * completes.
 * <p>
 * Used by engines that support multi-turn conversations (GPT, GROK).
 */
@Slf4j
@Component
@AllArgsConstructor
public final class AiConversationStrategy implements JobSearchStrategy {

  private final AiConversationStateMachine conversationStateMachine;
  private final TemplateRenderer templateRenderer;

  @Override
  public CompletableFuture<AiClientResponse> searchAsync(
      JobSearchRequest request,
      Executor executor,
      Function<JobSearchRequest, AiClientResponse> searchSync,
      Consumer<JobSearchRequest> cleanup
  ) {
    return conversationStateMachine.processAsync(
        request,
        executor,
        searchSync::apply,
        this::generateRejectedJobsPrompt,
        this::createRetryRequest,
        cleanup::accept
    );
  }

  private String generateRejectedJobsPrompt(List<Job> rejectedJobs) {
    List<String> rejectedUrls = rejectedJobs.stream()
        .map(Job::getUrl)
        .toList();
    return templateRenderer.getPrompt(PromptType.USER_PROMPT_JOB_BLAME_1,
        "timestamp", Instant.now(),
        "invalid_urls", rejectedUrls);
  }

  private JobSearchRequest createRetryRequest(JobSearchRequest originalRequest, AiClientResponse prevResponse, String newPrompt) {
    JobSearchRequest.Builder builder = originalRequest.toBuilder()
        .userPrompt(newPrompt)
        .fileId(null);
    if (builder instanceof ConversationBuilder cb) {
      cb.prevResponseId(prevResponse.getId());
    }
    return builder.build();
  }

}
