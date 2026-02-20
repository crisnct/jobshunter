package com.jobshunter.model;

public enum EngineType {
  GPT,
  GROK,
  GEMINI,
  SERP,
  SCRAPER;

  public static EngineType lookup(String engine) {
    for (EngineType type : EngineType.values()) {
      if (type.name().equalsIgnoreCase(engine)) {
        return type;
      }
    }
    return null;
  }

  public boolean isAiProvider() {
    return this != EngineType.SERP;
  }
}
