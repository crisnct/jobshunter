package com.jobshunter.service.retry;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Component
public class RetryTemplate {

  /** Upper bound for a server-requested wait, so a bogus Retry-After can not park a worker for minutes. */
  static final long MAX_RETRY_AFTER_MILLIS = 60_000;

  public <T> T execute(RetryPolicy<T> policy, String clientName, Supplier<T> supplier) {
    Throwable lastError = null;
    T lastResult = null;
    String caller = clientName + "-" + policy.name();
    for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
      Throwable attemptError = null;
      try {
        log.debug("🔁 {} Retry attempt {}/{}", caller, attempt, policy.maxAttempts());
        T result = supplier.get();
        lastResult = result;
        if (policy.successCondition().test(result)) {
          log.debug("✅ {} Retry succeeded on attempt {}/{}", caller, attempt, policy.maxAttempts());
          return result;
        } else {
          log.debug("❌ {} Retry condition not met on attempt {}/{}", caller, attempt, policy.maxAttempts());
        }
      } catch (Throwable ex) {
        lastError = ex;
        attemptError = ex;
        if (!policy.retryOnException().test(ex)) {
          log.error("💥 {} Exception not retryable, aborting retry", caller, ex);
          throw ex;
        }
        log.warn("💥 {} Retryable exception on attempt {}/{}: {}", caller, attempt, policy.maxAttempts(), ex.toString());
        if (attempt == policy.maxAttempts() - 1) {
          throw ex;
        }
      }

      if (attempt < policy.maxAttempts() && policy.delayMillis() > 0) {
        sleep(Math.max(policy.delayMillis(), retryAfterMillis(attemptError)), attempt);
      }
    }

    log.warn("⚠️ {} Retry exhausted after {} attempts, returning last result. Last error: {}",
        caller,
        policy.maxAttempts(),
        lastError != null ? lastError.toString() : "none"
    );

    return lastResult != null ? lastResult : policy.fallback();
  }

  /**
   * Honors the {@code Retry-After} header (seconds) of an HTTP 429, capped at {@link #MAX_RETRY_AFTER_MILLIS}; 0 for anything else. Rejected
   * requests are not billed by the providers, so waiting as told is cheaper than burning the remaining attempts.
   */
  static long retryAfterMillis(Throwable error) {
    if (error instanceof HttpClientErrorException.TooManyRequests tooMany) {
      String retryAfter = tooMany.getResponseHeaders() == null ? null : tooMany.getResponseHeaders().getFirst("Retry-After");
      if (retryAfter != null) {
        try {
          return Math.min(Long.parseLong(retryAfter.trim()) * 1000, MAX_RETRY_AFTER_MILLIS);
        } catch (NumberFormatException e) {
          log.debug("Ignoring non numeric Retry-After header: {}", retryAfter);
        }
      }
    }
    return 0;
  }

  private void sleep(long millis, int attempt) {
    long jitter = ThreadLocalRandom.current().nextLong(1000);
    long total = millis + jitter;
    try {
      log.trace("⏳ Waiting {} ms before next retry (after attempt {})", total, attempt);
      Thread.sleep(total);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("⚠️ Retry interrupted during sleep, aborting retries");
    }
  }
}
