package com.jobshunter.service.snippets;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Data;

@Data
class DummyJob {

  private String id;
  private String status;
  private String data;

  public DummyJob(String id) {
    this.id = id;
    this.status = "CREATED";
    this.data = "";
  }

  @Override
  public String toString() {
    return "Job{" + "id='" + id + "', status='" + status + "', data='" + data + "'}";
  }
}

public class JobPipeline {

  private static final ExecutorService executor = Executors.newFixedThreadPool(4);

  static void main(String[] args) {
    List<CompletableFuture<DummyJob>> jobs = new ArrayList<>();
    IntStream.range(0, 3).forEach((i) -> {
      jobs.add(CompletableFuture.supplyAsync(() -> createJob("JOB-" + i)));
    });

    jobs.stream()
        .map(job ->
            job.thenApplyAsync(JobPipeline::validateJob, executor)
            .thenApplyAsync(JobPipeline::processJob, executor)
            .thenApplyAsync(JobPipeline::finalizeJob, executor)
            .exceptionally(ex -> {
              System.err.println("Pipeline failed: " + ex.getMessage());
              return null;
            }))
        .collect(Collectors.toList());

    CompletableFuture.allOf(jobs.toArray(new CompletableFuture[0])).join();

    executor.shutdown();
  }

  private static DummyJob createJob(String jobId) {
    System.out.println("[CREATE] Creating job: " + jobId);
    return new DummyJob(jobId);
  }

  private static DummyJob validateJob(DummyJob job) {
    System.out.println("[VALIDATE] Validating job: " + job.getId());
    job.setStatus("VALIDATED");
    job.setData("validation_passed");
    return job;
  }

  private static DummyJob processJob(DummyJob job) {
    System.out.println("[PROCESS] Processing job: " + job.getId());
    job.setStatus("PROCESSING");
    job.setData(job.getData() + " -> processed");
    return job;
  }

  private static DummyJob finalizeJob(DummyJob job) {
    System.out.println("[FINALIZE] Finalizing job: " + job.getId());
    job.setStatus("COMPLETED");
    job.setData(job.getData() + " -> finalized");
    return job;
  }
}