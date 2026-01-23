package com.jobshunter.service.retry;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RetryTemplate {

  public <T> T execute(RetryPolicy<T> policy, String clientName, Supplier<T> supplier) {
    Throwable lastError = null;
    String caller = clientName + "-" + policy.name();
    for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
      try {
        log.debug("🔁 {} Retry attempt {}/{}", caller, attempt, policy.maxAttempts());
        T result = supplier.get();
        if (policy.successCondition().test(result)) {
          log.debug("✅ {} Retry succeeded on attempt {}/{}", caller, attempt, policy.maxAttempts());
          return result;
        } else {
          log.debug("❌ {} Retry condition not met on attempt {}/{}", caller, attempt, policy.maxAttempts());
        }
      } catch (Throwable ex) {
        lastError = ex;
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
        sleep(policy.delayMillis(), attempt);
      }
    }

    log.warn("⚠️ {} Retry exhausted after {} attempts, returning fallback. Last error: {}",
        caller,
        policy.maxAttempts(),
        lastError != null ? lastError.toString() : "none"
    );

    return policy.fallback();
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
