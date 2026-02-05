package com.jobshunter.service.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

/**
 * Service for recording job hunting business metrics.
 * Tracks jobs found, validated, rejected, errors, and score distribution.
 */
@Service
public class JobMetricsService {

  private final Counter jobsFoundCounter;
  private final Counter jobsValidatedCounter;
  private final Counter jobsRejectedCounter;
  private final Counter jobsErrorsCounter;
  private final DistributionSummary scoreDistribution;

  public JobMetricsService(MeterRegistry registry) {
    this.jobsFoundCounter = Counter.builder("job.hunt.found")
        .description("Total number of jobs found from AI providers")
        .register(registry);

    this.jobsValidatedCounter = Counter.builder("job.hunt.validated")
        .description("Number of jobs that passed validation pipeline")
        .register(registry);

    this.jobsRejectedCounter = Counter.builder("job.hunt.rejected")
        .description("Number of jobs rejected during validation")
        .register(registry);

    this.jobsErrorsCounter = Counter.builder("job.hunt.errors")
        .description("Number of jobs that failed with errors")
        .register(registry);

    this.scoreDistribution = DistributionSummary.builder("job.hunt.score")
        .description("Distribution of job match scores")
        .publishPercentiles(0.5, 0.75, 0.9, 0.95, 0.99)
        .register(registry);
  }

  /**
   * Record the number of jobs found from AI providers.
   *
   * @param count number of jobs found
   */
  public void recordJobsFound(int count) {
    jobsFoundCounter.increment(count);
  }

  /**
   * Record a single validated job.
   */
  public void recordJobValidated() {
    jobsValidatedCounter.increment();
  }

  /**
   * Record a single rejected job.
   */
  public void recordJobRejected() {
    jobsRejectedCounter.increment();
  }

  /**
   * Record a job that failed with an error.
   */
  public void recordJobError() {
    jobsErrorsCounter.increment();
  }

  /**
   * Record a job match score.
   *
   * @param score the match score (0-100)
   */
  public void recordScore(int score) {
    scoreDistribution.record(score);
  }

  /**
   * Record batch results from pipeline processing.
   *
   * @param validated number of validated jobs
   * @param rejected  number of rejected jobs
   * @param errors    number of jobs with errors
   */
  public void recordBatchResults(int validated, int rejected, int errors) {
    jobsValidatedCounter.increment(validated);
    jobsRejectedCounter.increment(rejected);
    jobsErrorsCounter.increment(errors);
  }
}
