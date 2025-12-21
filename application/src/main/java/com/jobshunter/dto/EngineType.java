package com.jobshunter.dto;

public enum EngineType {
  GPT4,
  GPT5,
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
