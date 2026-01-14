package com.jobshunter.service.application.processors;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobsStateMachine {

  private final JobFakelUrFilter fakeUrlFilterProcessor;

  private final JobFetchProcessor fetchPageProcessor;

  private final JobBodyExtractorProcessor bodyExtractorProcessor;

  private final JobValidator validatorProcessor;

  private final JobScoring<?> geminiScoringProcessor;

  private final JobScoring<?> grokScoringProcessor;

  private final JobScoring<?> gptScoringProcessor;

  private final Executor urlFetchRestClientExecutor;

  private final Executor geminiExecutor;

  private final Executor grokExecutor;

  private final Executor gptExecutor;

  private final Executor jobProcessingExecutor;

  public JobsStateMachine(
      JobValidator validatorProcessor,
      JobFetchProcessor fetchPageProcessor,
      JobFakelUrFilter fakeUrlFilterProcessor,
      JobBodyExtractorProcessor bodyExtractorProcessor,

      @Qualifier("jobScoringGemini")
      JobScoring<?> geminiScoringProcessor,
      @Qualifier("jobScoringGpt")
      JobScoring<?> gptScoringProcessor,
      @Qualifier("jobScoringGrok")
      JobScoring<?> grokScoringProcessor,

      @Qualifier("urlFetchRestClientExecutor") Executor urlFetchRestClientExecutor,
      @Qualifier("geminiSearchExecutor") Executor geminiExecutor,
      @Qualifier("grokSearchExecutor") Executor grokExecutor,
      @Qualifier("gptSearchExecutor") Executor gptExecutor,
      @Qualifier("jobProcessingExecutor") Executor jobProcessingExecutor
  ) {
    this.geminiScoringProcessor = geminiScoringProcessor;
    this.gptScoringProcessor = gptScoringProcessor;
    this.validatorProcessor = validatorProcessor;
    this.grokScoringProcessor = grokScoringProcessor;
    this.fetchPageProcessor = fetchPageProcessor;
    this.urlFetchRestClientExecutor = urlFetchRestClientExecutor;
    this.bodyExtractorProcessor = bodyExtractorProcessor;
    this.fakeUrlFilterProcessor = fakeUrlFilterProcessor;
    this.jobProcessingExecutor = jobProcessingExecutor;
    this.geminiExecutor = geminiExecutor;
    this.gptExecutor = gptExecutor;
    this.grokExecutor = grokExecutor;
  }

  //TODO replace by the other method with similar name
  public List<Job> processAsync(CompletableFuture<List<Job>> futureJobs, UserEntity user) {
    CompletableFuture<List<JobContext>> allJobs = futureJobs
        .thenCompose(jobsList -> {
          List<CompletableFuture<JobContext>> pipelined = jobsList.stream()
              .map(job -> applyJobPipeline(job, user, true))
              .toList();

          return CompletableFuture.allOf(pipelined.toArray(CompletableFuture[]::new))
              .thenApply(v -> pipelined.stream()
                  .map(CompletableFuture::join)
                  .toList()
              );
        });

    List<JobContext> result = allJobs.join();
    this.logResults(result, user.getUsername());
    return result.stream()
        .filter(ctx -> !ctx.isFailed() && ctx.isAccepted())
        .map(JobContext::getJob)
        .toList();
  }

  public CompletableFuture<List<JobContext>> processAsyncWithContext(CompletableFuture<List<Job>> futureJobs, UserEntity user, boolean scoring) {
    CompletableFuture<List<JobContext>> allJobs = futureJobs
        .thenCompose(jobsList -> {
          List<CompletableFuture<JobContext>> pipelined = jobsList.stream()
              .map(job -> applyJobPipeline(job, user, scoring))
              .toList();

          return CompletableFuture.allOf(pipelined.toArray(CompletableFuture[]::new))
              .thenApply(v -> pipelined.stream()
                  .map(CompletableFuture::join)
                  .toList()
              );
        });

    return allJobs.thenApply(contexts -> {
      this.logResults(contexts, user.getUsername());
      return contexts;
    });
  }

  //TODO do optimization to not pass again the jobs through state machine the second time
  // through all the stages but only for scoring.
  private CompletableFuture<JobContext> applyJobPipeline(Job job, UserEntity user, boolean scoring) {
    return CompletableFuture.supplyAsync(() -> new JobContext(job, user), jobProcessingExecutor)
        .thenApplyAsync(ctx -> ctx.isSkipProcessors() ? ctx : fakeUrlFilterProcessor.processAsync(ctx), jobProcessingExecutor)
        .thenApplyAsync(ctx -> ctx.isSkipProcessors() ? ctx : fetchPageProcessor.processAsync(ctx), urlFetchRestClientExecutor)
        .thenApplyAsync(ctx -> ctx.isSkipProcessors() ? ctx : bodyExtractorProcessor.processAsync(ctx), jobProcessingExecutor)
        .thenApplyAsync(ctx -> ctx.isSkipProcessors() ? ctx : validatorProcessor.processAsync(ctx), jobProcessingExecutor)
        .thenApplyAsync(ctx -> (ctx.isSkipProcessors() || !scoring) ? ctx : gptScoringProcessor.processAsync(ctx), gptExecutor)
        .handle((jc, ex) -> {
          if (ex != null) {
            log.error("Pipeline failed for job {}", job != null ? job.getUrl() : "unknown", ex);
            return JobContext.failed(job, user, ex);
          }
          if (jc == null) {
            IllegalStateException error = new IllegalStateException("Pipeline returned null JobContext");
            log.error("Pipeline returned null context for job {}", job != null ? job.getUrl() : "unknown");
            return JobContext.failed(job, user, error);
          }
          return jc;
        });
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
        validLinksBuilder.append("score:");
        validLinksBuilder.append(jc.getJob().getScore());
        validLinksBuilder.append("%, ");
        validLinksBuilder.append(jc.getJob().getSource());
        validLinksBuilder.append(": ");
        validLinksBuilder.append(jc.getJob().getUrl());
      });
    }
    StringBuilder rejectedLinksBuilder = new StringBuilder();
    if (rejected > 0) {
      result.stream().filter(p -> !p.isAccepted()).forEach(jc -> {
        if (!rejectedLinksBuilder.isEmpty()) {
          rejectedLinksBuilder.append("\n");
        }
        rejectedLinksBuilder.append(jc.getJob().getSource());
        rejectedLinksBuilder.append(": ");
        rejectedLinksBuilder.append(jc.getJob().getUrl());
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

    log.info("""
        \n--- Job search results for {} -----------------------------------------
        Total jobs found: {}
        ✅Valid links: {}
        🔍Rejected: {}
        ❌Errors: {}
        
        ✅Valid links:
        {}
        
        🔍Rejected links:
        {}
        
        Errors:
        {}---
        """, username, result.size(), acceptedUrls, rejected, errors, validLinksBuilder, rejectedLinksBuilder, errorBuilder
    );
  }

}
