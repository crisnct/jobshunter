package com.jobshunter.security.rateLimit;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.ApplicationProperties.RateLimitPolicy;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

//@formatter:off
/**
 * In-memory buckets keyed by client identifier (IP, API key, user id).
 * Suitable for single-instance deployments.
 * For multi-instance, replace this with a distributed bucket store (e.g. Redis).
 */
//@formatter:on
@Component
public class InMemoryRateLimiter {

  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final long capacity;
  private final Duration window;

  public InMemoryRateLimiter(ApplicationProperties properties) {
    RateLimitPolicy rateLimit = properties.getJobsHunter().getRateLimit();
    this.capacity = rateLimit.getCapacity();
    this.window = rateLimit.getWindow();
  }

  public Bucket resolveBucket(String key) {
    return buckets.computeIfAbsent(key, ignored -> newBucket());
  }

  private Bucket newBucket() {
    return Bucket.builder()
        .addLimit(limit -> limit
            .capacity(capacity)
            .refillGreedy(capacity, window))
        .build();
  }
}
