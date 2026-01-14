package com.jobshunter.model;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.service.clients.browser.HttpFetchResult;
import java.util.Objects;
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
  private boolean validatedSuccessfully;
  private JobPhase phase;
  private boolean failed;
  private String finalizationMessage;

  public JobContext(Job job, UserEntity user) {
    this.job = job;
    this.user = user;
    this.phase = JobPhase.NEW;
  }

  public static JobContext failed(Job job, UserEntity user, Throwable t) {
    JobContext ctx = new JobContext(job, user);
    ctx.validatedSuccessfully = false;
    ctx.failJob(t != null ? t.getMessage() : "Unknown pipeline failure");
    return ctx;
  }

  public boolean isOkToRun(JobPhase phase) {
    Objects.requireNonNull(phase);
    return !failed && this.phase.ordinal() < phase.ordinal();
  }

  public boolean hasFetchResult() {
    return fetchResult != null && Strings.isNotBlank(fetchResult.body());
  }

  public void finalizeJob(String message) {
    this.finalizationMessage = message;
    this.phase = JobPhase.values()[JobPhase.values().length - 1];
  }

  public void failJob(String message) {
    this.failed = true;
    this.finalizationMessage = message;
    this.phase = JobPhase.values()[JobPhase.values().length - 1];
  }

}
