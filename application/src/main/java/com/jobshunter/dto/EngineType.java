package com.jobshunter.dto;

public enum EngineType {
  GPT4,
  GPT5,
  GEMINI_2_5_FLASH_LITE,
  GEMINI_2_5_FLASH,
  GEMINI_2_5_PRO,
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
