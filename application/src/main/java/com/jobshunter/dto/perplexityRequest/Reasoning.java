package com.jobshunter.dto.perplexityRequest;

public record Reasoning(String effort) {

  public Reasoning() {
    this("low");
  }
}
