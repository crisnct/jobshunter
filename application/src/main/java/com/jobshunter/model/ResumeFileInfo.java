package com.jobshunter.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record ResumeFileInfo(String fileId, String filename, Instant expireAt) {

  public ResumeFileInfo(String fileId, String filename, Instant expireAt) {
    this.fileId = fileId;
    this.filename = filename;
    this.expireAt = expireAt == null ? Instant.now().plus(30, ChronoUnit.DAYS) : expireAt;
  }
}
