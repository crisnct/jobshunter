package com.jobshunter.service.application.processors;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobPhase;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobsStateMachine {

  private final JobScoring jobScoring;

  private final JobsValidator jobsValidator;

  private final JobRedirection jobRedirection;

  private final Executor miscExecutor;

  private final Executor geminiExecutor;

  public JobsStateMachine(
      JobScoring jobScoring,
      JobsValidator jobsValidator,
      JobRedirection jobRedirection,
      @Qualifier("miscellaneousExecutor") Executor miscExecutor,
      @Qualifier("geminiSearchExecutor") Executor geminiExecutor
  ) {
    this.jobScoring = jobScoring;
    this.jobsValidator = jobsValidator;
    this.jobRedirection = jobRedirection;
    this.miscExecutor = miscExecutor;
    this.geminiExecutor = geminiExecutor;
  }

  public List<JobContext> processAsync(CompletableFuture<List<Job>> futureJobs, UserEntity user, String resumeFileId) {
    CompletableFuture<List<JobContext>> allJobs = futureJobs
        .thenCompose(jobsList -> {
          List<CompletableFuture<JobContext>> pipelined = jobsList.stream()
              .map(job -> applyJobPipeline(job, user, resumeFileId))
              .toList();

          return CompletableFuture.allOf(pipelined.toArray(CompletableFuture[]::new))
              .thenApply(v -> pipelined.stream()
                  .map(CompletableFuture::join)
                  .toList()
              );
        });

    List<JobContext> result = allJobs.join();
    this.logResults(result, user.getUsername());
    return result;
  }

  private void logResults(List<JobContext> result, String username) {
    long acceptedUrls = result.stream().filter(JobContext::isAccepted).count();
    long rejected = result.size() - acceptedUrls;
    long errors = result.stream().filter(JobContext::isFailed).count();

    StringBuilder validLinksBuilder = new StringBuilder();
    if (acceptedUrls > 0) {
      result.stream().filter(JobContext::isAccepted).forEach(jc -> {
        if (!validLinksBuilder.isEmpty()) {
          validLinksBuilder.append("\n");
        }
        validLinksBuilder.append(jc.getJob().getSource());
        validLinksBuilder.append(": ");
        validLinksBuilder.append(jc.getJob().getUrl());
      });
    }
    StringBuilder errorBuilder = new StringBuilder();
    if (errors > 0) {
      result.stream().filter(JobContext::isFailed).forEach(jc -> {
        if (!errorBuilder.isEmpty()) {
          errorBuilder.append("\n");
        }
        errorBuilder.append(jc.getFailureMessage());
      });
    }
    result.stream().filter(jc -> jc.getPhase().ordinal() < JobPhase.SCORED.ordinal()).forEach(jc -> {
      errorBuilder.append("\nWrong phase: ");
      errorBuilder.append(jc.getPhase().name());
      errorBuilder.append(" ");
      errorBuilder.append(jc.getJob().getPromptId());
      errorBuilder.append(" ");
      errorBuilder.append(jc.getFailureMessage());
    });

    log.info("""
        \n--- Job search results for {} -----------------------------------------
        Total jobs found: {}
        ✅Valid links: {}
        🔍Rejected: {}
        ❌Errors: {}
        
        ✅Valid links:
        {}
        
        Errors:
        {}---
        """, username, result.size(), acceptedUrls, rejected, errors, validLinksBuilder, errorBuilder
    );
  }

  private CompletableFuture<JobContext> applyJobPipeline(Job job, UserEntity user, String resumeFileId) {
    return CompletableFuture.supplyAsync(() -> new JobContext(job, user, resumeFileId), miscExecutor)
        .thenApplyAsync(jobRedirection::processAsync, miscExecutor)
        .thenApplyAsync(jobsValidator::processAsync, miscExecutor)
        .thenApplyAsync(jobScoring::processAsync, geminiExecutor)
        .handleAsync((jc, ex) -> {
          if (ex != null) {
            log.error("Pipeline failed for job {}: {}", job != null ? job.getUrl() : "unknown", ex.getMessage(), ex);
            return JobContext.failed(job, user, resumeFileId, ex);
          }
          if (jc == null) {
            log.error("Pipeline returned null context for job {}", job != null ? job.getUrl() : "unknown");
            return JobContext.failed(job, user, resumeFileId, new IllegalStateException("Pipeline returned null JobContext"));
          }
          return jc;
        }, miscExecutor);
  }

}
