package com.jobshunter.dto.gptRequest;

public record Reasoning(String effort) {

  public Reasoning() {
    this("medium");
  }
}
