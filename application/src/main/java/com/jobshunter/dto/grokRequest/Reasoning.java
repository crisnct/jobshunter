package com.jobshunter.dto.grokRequest;

public record Reasoning(String effort) {

  public Reasoning() {
    this("low");
  }
}
