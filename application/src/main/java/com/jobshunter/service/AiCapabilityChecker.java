package com.jobshunter.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.model.AiCapabilityType;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for checking AI model capabilities with caching.
 * Centralizes the capability check logic that was previously duplicated
 * across multiple payload builder classes.
 */
@Slf4j
public final class AiCapabilityChecker {

  private static final Cache<String, Boolean> CACHE = Caffeine.newBuilder()
      .maximumSize(500)
      .expireAfterWrite(Duration.ofHours(4))
      .build();

  private AiCapabilityChecker() {
  }

  /**
   * Check if a capability is enabled for the given AI model.
   * Results are cached to avoid repeated lookups.
   *
   * @param model the AI model entity
   * @param type  the capability type to check
   * @return true if the capability is enabled, false otherwise
   */
  public static boolean isEnabled(AiModelEntity model, AiCapabilityType type) {
    String key = model.getId() + ":" + type.name();
    return CACHE.get(key, k -> {
      boolean enabled = model.isEnabledCapability(type);
      if (!enabled) {
        log.debug("{} capability is not supported by model {}", type, model.getModel());
      }
      return enabled;
    });
  }
}
