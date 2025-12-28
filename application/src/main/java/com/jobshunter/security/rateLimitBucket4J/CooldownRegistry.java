package com.jobshunter.security.rateLimitBucket4J;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class CooldownRegistry {

  private final ConcurrentHashMap<String, Instant> blockedUntil = new ConcurrentHashMap<>();

  public void block(String key, Duration duration) {
    blockedUntil.put(key, Instant.now().plus(duration));
  }

  public boolean isBlocked(String key) {
    Instant until = blockedUntil.get(key);
    if (until == null) {
      return false;
    }

    if (Instant.now().isAfter(until)) {
      blockedUntil.remove(key);
      return false;
    }

    return true;
  }

  public long secondsLeft(String key) {
    Instant until = blockedUntil.get(key);
    if (until == null) {
      return 0;
    }
    return Math.max(0, Duration.between(Instant.now(), until).getSeconds());
  }
}
