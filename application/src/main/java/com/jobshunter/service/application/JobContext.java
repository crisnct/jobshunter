package com.jobshunter.service.application;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.model.Job;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class JobContext {

  private final Job job;
  private final UserEntity user;
  private final String resumeFileId;

  private boolean accepted;
  private volatile JobPhase phase;
  private boolean failed;
  private String failureMessage;

  protected JobContext(Job job, UserEntity user, String resumeFileId) {
    this.job = job;
    this.user = user;
    this.resumeFileId = resumeFileId;
    this.phase = JobPhase.NEW;
  }

  public static JobContext failed(Job job, UserEntity user, String resumeFileId, Throwable t) {
    JobContext ctx = new JobContext(job, user, resumeFileId);
    ctx.failed = true;
    ctx.failureMessage = t != null ? t.getMessage() : "Unknown pipeline failure";
    return ctx;
  }

  public void setPhase(JobPhase phase) {
    if (phase.ordinal() - this.phase.ordinal() != 1) {
      log.error("Unexpected job order processing old: {}, new: {}", this.phase.name(), phase.name());
    }
    this.phase = phase;
  }
}
