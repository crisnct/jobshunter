package com.jobshunter.model;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.service.clients.browser.HttpFetchResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

@Data
@Slf4j
public class JobContext {

  private final Job job;
  private String body;

  private final UserEntity user;

  private HttpFetchResult fetchResult;
  private String host;
  private String description;
  private boolean accepted;
  private boolean realUrl;
  private JobPhase phase;
  private boolean failed;
  private String failureMessage;
  private boolean skipProcessors;

  public JobContext(Job job, UserEntity user) {
    this.job = job;
    this.user = user;
    this.phase = JobPhase.NEW;
  }

  public static JobContext failed(Job job, UserEntity user, Throwable t) {
    JobContext ctx = new JobContext(job, user);
    ctx.failed = true;
    ctx.failureMessage = t != null ? t.getMessage() : "Unknown pipeline failure";
    ctx.accepted = false;
    return ctx;
  }

  public void setPhase(JobPhase phase) {
    if (phase.ordinal() - this.phase.ordinal() != 1) {
      log.error("Unexpected job order processing old: {}, new: {}", this.phase.name(), phase.name());
    }
    this.phase = phase;
  }

  public boolean hasFetchResult() {
    return fetchResult != null && Strings.isNotBlank(fetchResult.body());
  }
}
