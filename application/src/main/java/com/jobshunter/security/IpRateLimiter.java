package com.jobshunter.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

public final class IpRateLimiter {

  private static final ConcurrentHashMap<String, Bucket> BUCKETS = new ConcurrentHashMap<>();

  private static final Bandwidth LIMIT =
      Bandwidth.simple(3, Duration.ofSeconds(1)); // 3 req / sec / IP

  private IpRateLimiter() {
  }

  public static boolean allowRequest(String key) {
    Bucket bucket = BUCKETS.computeIfAbsent(key, k -> Bucket.builder().addLimit(LIMIT).build());
    return bucket.tryConsume(1);
  }
}
