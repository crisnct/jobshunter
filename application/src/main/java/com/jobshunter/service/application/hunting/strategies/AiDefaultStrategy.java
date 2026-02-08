package com.jobshunter.service.application.hunting.strategies;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.dto.JobSearchRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.service.application.hunting.JobSearchStrategy;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Simple one-shot search strategy.
 * <p>
 * Wraps the synchronous search in a {@link CompletableFuture} with a 30-minute
 * timeout and resilient error handling. No conversation retries are performed.
 * <p>
 * Used by engines that do not support multi-turn conversations (SERP, Gemini).
 */
@Slf4j
@Component
public final class AiDefaultStrategy implements JobSearchStrategy {

  @Override
  public CompletableFuture<AiClientResponse> searchAsync(
      JobSearchRequest request,
      Executor executor,
      Function<JobSearchRequest, AiClientResponse> searchSync,
      Consumer<JobSearchRequest> cleanup
  ) {
    return CompletableFuture.supplyAsync(() -> searchSync.apply(request), executor)
        .orTimeout(30, TimeUnit.MINUTES)
        .exceptionally(throwable -> {
          AiModelEntity aiModel = request.getOrder().getModel();
          Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;

          if (cause instanceof TimeoutException) {
            log.warn("⏱️ Timeout exceeded for user {}, engine: {}, model: {}, prompt: {}",
                request.getOrder().getUser().getUsername(),
                aiModel.getProvider(), aiModel.getModel(),
                request.getUserPrompt());
          } else if (cause instanceof RequestNotPermitted) {
            log.error("❌ Rate limit exceeded for user {}, engine: {}, model: {}",
                request.getOrder().getUser().getUsername(), aiModel.getProvider(), aiModel.getModel());
          } else {
            log.error("Unexpected error at gathering jobs from model {}: {} for prompt {}", aiModel.getModel(),
                throwable.getMessage(), request.getUserPrompt());
          }
          return new AiClientResponse();
        })
        .whenComplete((_, _) -> cleanup.accept(request));
  }

}
