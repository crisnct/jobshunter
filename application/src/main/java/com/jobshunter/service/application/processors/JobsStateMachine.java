package com.jobshunter.service.application.processors;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobMetadataType;
import com.jobshunter.model.JobPhase;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.metrics.JobMetricsService;
import com.jobshunter.service.application.processors.validation.JobValidatorProcessor;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobsStateMachine {

  private final Executor jobProcessingExecutor;

  private final List<PipelineStep> pipelineSteps;

  private final JobMetricsService metricsService;

  public JobsStateMachine(
      JobValidatorProcessor validatorProcessor,
      JobFetchProcessor fetchPageProcessor,
      JobBasicCheckProcessor fakeUrlFilterProcessor,
      JobBodyExtractorProcessor bodyExtractorProcessor,
      JobScoringProcessor scoringProcessor,
      JobMetricsService metricsService,
      @Qualifier("urlFetchPlaywrightExecutor") Executor urlFetchPlaywrightExecutor,
      @Qualifier("geminiSearchExecutor") Executor geminiExecutor,
      @Qualifier("grokSearchExecutor") Executor grokExecutor,
      @Qualifier("gptSearchExecutor") Executor gptExecutor,
      @Qualifier("jobProcessingExecutor") Executor jobProcessingExecutor
  ) {
    this.jobProcessingExecutor = jobProcessingExecutor;
    this.metricsService = metricsService;

    Executor scoringExecutor = (switch (JobScoringProcessor.ENGINE_SELECTION.type()) {
      case GEMINI -> geminiExecutor;
      case GROK -> grokExecutor;
      case GPT -> gptExecutor;
      default -> throw new IllegalStateException("Unexpected value: " + JobScoringProcessor.ENGINE_SELECTION.type());
    });

    this.pipelineSteps = List.of(
        new PipelineStep(JobPhase.BASIC_CHECK, fakeUrlFilterProcessor, jobProcessingExecutor),
        new PipelineStep(JobPhase.FETCH, fetchPageProcessor, urlFetchPlaywrightExecutor),
        new PipelineStep(JobPhase.BODY_EXTRACTION, bodyExtractorProcessor, jobProcessingExecutor),
        new PipelineStep(JobPhase.VALIDATION, validatorProcessor, jobProcessingExecutor),
        new PipelineStep(JobPhase.SCORING, scoringProcessor, scoringExecutor)
    );
  }

  public CompletableFuture<List<JobContext>> processAsync(CompletableFuture<List<Job>> futureJobs, UserEntity user, SearchJobOrder order) {
    CompletableFuture<List<JobContext>> allJobs = futureJobs
        .thenCompose(jobsList -> {
          List<CompletableFuture<JobContext>> pipelined = jobsList.stream()
              .map(job -> {
                    if (job.getMetadata(JobMetadataType.APPROVED_BY_CONVERSATION_STATE_MACHINE) != null) {
                      JobContext jc = new JobContext(job, user, order);
                      jc.setValidatedSuccessfully(true);
                      jc.finalizeJob("the job was already passed through JobsStateMachine in Conversation State Machine");
                      return CompletableFuture.completedFuture(jc);
                    } else {
                      return applyJobPipeline(job, user, order);
                    }
                  }
              )
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

  private CompletableFuture<JobContext> applyJobPipeline(Job job, UserEntity user, SearchJobOrder order) {
    CompletableFuture<JobContext> pipeline = CompletableFuture.supplyAsync(() -> new JobContext(job, user, order), jobProcessingExecutor);
    for (PipelineStep step : pipelineSteps) {
      pipeline = pipeline.thenApplyAsync(ctx -> ctx.isOkToRun(step.phase()) ? step.processor().processAsync(ctx) : ctx, step.executor());
    }
    return pipeline.handle((jc, ex) -> {
      if (ex != null) {
        log.error("Pipeline failed for job {}", job.getUrl(), ex);
        return JobContext.failed(job, user, ex);
      }
      if (jc == null) {
        IllegalStateException error = new IllegalStateException("Pipeline returned null JobContext");
        log.error("Pipeline returned null context for job {}", job.getUrl());
        return JobContext.failed(job, user, error);
      }
      return jc;
    });
  }

  private void logResults(List<JobContext> result, String username) {
    List<JobContext> validated = result.stream().filter(JobContext::isValidatedSuccessfully).toList();

    long rejected = result.size() - validated.size();
    long errors = result.stream().filter(JobContext::isFailed).count();

    // Record metrics
    metricsService.recordJobsFound(result.size());
    metricsService.recordBatchResults(validated.size(), (int) rejected, (int) errors);

    // Record individual scores for validated jobs
    result.stream()
        .filter(JobContext::isValidatedSuccessfully)
        .forEach(jc -> metricsService.recordScore(jc.getJob().getScore()));

    StringBuilder validLinksBuilder = new StringBuilder();
    if (!validated.isEmpty()) {
      validated.forEach(jc -> {
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
      result.stream().filter(p -> !p.isValidatedSuccessfully()).forEach(jc -> {
        if (!rejectedLinksBuilder.isEmpty()) {
          rejectedLinksBuilder.append("\n");
        }
        rejectedLinksBuilder.append(jc.getJob().getSource());
        rejectedLinksBuilder.append(": ");
        rejectedLinksBuilder.append(jc.getJob().getUrl());
        rejectedLinksBuilder.append(", reason: ");
        rejectedLinksBuilder.append(jc.getFinalizationMessage());
      });
    }

    StringBuilder errorBuilder = new StringBuilder();
    if (errors > 0) {
      result.stream().filter(JobContext::isFailed).forEach(jc -> {
        if (!errorBuilder.isEmpty()) {
          errorBuilder.append("\n");
        }
        errorBuilder.append(jc.getFinalizationMessage());
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
        """, username, result.size(), validated.size(), rejected, errors, validLinksBuilder, rejectedLinksBuilder, errorBuilder
    );
  }

  private record PipelineStep(JobPhase phase, JobProcessor processor, Executor executor) {

  }

}
