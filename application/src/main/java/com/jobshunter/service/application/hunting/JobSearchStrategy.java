package com.jobshunter.service.application.hunting;

import com.jobshunter.dto.JobSearchRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.service.application.hunting.strategies.AiConversationStrategy;
import com.jobshunter.service.application.hunting.strategies.AiDefaultStrategy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Strategy interface for executing a single job search request.
 * <p>
 * Implementations define <em>how</em> a search is executed (simple one-shot vs.
 * conversation-based with retries), while the concrete {@link JobHunting}
 * implementations define <em>what</em> to search (prompts, roles, etc.) and
 * provide engine-specific configuration.
 *
 * @see AiDefaultStrategy
 * @see AiConversationStrategy
 */
public sealed interface JobSearchStrategy permits AiDefaultStrategy, AiConversationStrategy {

  /**
   * Executes an asynchronous job search for a single request.
   *
   * @param request               the search request to execute
   * @param executor              the executor to run the async computation on
   * @param searchSync            synchronous search function provided by the engine
   * @param cleanup   callback for conversation cleanup (may be a no-op)
   * @return a future containing the search response
   */
  CompletableFuture<AiClientResponse> searchAsync(
      JobSearchRequest request,
      Executor executor,
      Function<JobSearchRequest, AiClientResponse> searchSync,
      Consumer<JobSearchRequest> cleanup
  );

}
