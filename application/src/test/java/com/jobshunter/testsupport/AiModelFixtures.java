package com.jobshunter.testsupport;

import com.jobshunter.database.entities.AiCapabilityEntity;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.AiModelsCapabilityEntity;
import com.jobshunter.model.AiCapabilityType;
import com.jobshunter.model.EngineType;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Builds in-memory {@link AiModelEntity} instances. Every model gets a fresh id because {@code AiCapabilityChecker} caches the answers per model
 * id for the whole JVM.
 */
public final class AiModelFixtures {

  private static final AtomicLong IDS = new AtomicLong(9_000);

  private AiModelFixtures() {
  }

  /** A Perplexity model that declares exactly the given capabilities as enabled. */
  public static AiModelEntity perplexityModel(AiCapabilityType... enabled) {
    AiModelEntity model = new AiModelEntity(EngineType.PERPLEXITY, "openai/gpt-6-luna");
    model.setId(IDS.incrementAndGet());
    model.setContextWindow(272_000);
    model.setTokensPerChar(0.3f);
    model.setInputPrice(0.2);
    model.setOutputPrice(0.75);
    model.setToolPrice(2500);
    for (AiCapabilityType type : enabled) {
      AiCapabilityEntity capability = new AiCapabilityEntity();
      capability.setId(IDS.incrementAndGet());
      capability.setType(type);
      capability.setValueType("BOOLEAN");
      model.getCapabilities().add(new AiModelsCapabilityEntity(model, capability));
    }
    return model;
  }

  /** The capabilities configured by the Liquibase changeset of the real model. */
  public static AiModelEntity defaultPerplexityModel() {
    return perplexityModel(AiCapabilityType.WEB_SEARCH, AiCapabilityType.SYSTEM_PROMPT, AiCapabilityType.RESPONSE_SCHEMA);
  }
}
