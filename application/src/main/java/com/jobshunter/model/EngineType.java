package com.jobshunter.model;

public enum EngineType {
  GPT,
  GEMINI,
  SERP;

  public static EngineType lookup(String engine) {
    for (EngineType type : EngineType.values()) {
      if (type.name().equalsIgnoreCase(engine)) {
        return type;
      }
    }
    return null;
  }
}
