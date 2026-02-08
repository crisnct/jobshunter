package com.jobshunter.service.application.hunting.hunters;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.dto.JobSearchRequest;
import com.jobshunter.dto.SerpSearchRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.hunting.JobByPromptHunting;
import com.jobshunter.service.application.hunting.JobHunting;
import com.jobshunter.service.application.hunting.strategies.AiDefaultStrategy;
import com.jobshunter.service.clients.AiJobsClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class SerpJobHunting implements JobHunting, JobByPromptHunting {

  private final AiJobsClient<SerpSearchRequest> jobsClient;
  private final Executor executor;
  private final AiDefaultStrategy jobSearchStrategy;

  public SerpJobHunting(
      @Qualifier("JobsClientSerp") AiJobsClient<SerpSearchRequest> serpClient,
      @Qualifier("serpExecutor") Executor serpExecutor,
      AiDefaultStrategy strategy
  ) {
    this.jobsClient = serpClient;
    this.executor = serpExecutor;
    this.jobSearchStrategy = strategy;
  }

  @Override
  public EngineType getEngineType() {
    return EngineType.SERP;
  }

  // ---------------------------------------------------------------------------
  // Request building
  // ---------------------------------------------------------------------------

  private SerpSearchRequest createRequest(SearchJobOrder order, String prompt) {
    return SerpSearchRequest.builder(order).userPrompt(prompt).build();
  }

  // ---------------------------------------------------------------------------
  // Primary search orchestration
  // ---------------------------------------------------------------------------

  @Override
  public CompletableFuture<List<Job>> searchJobsAsync(SearchJobOrder order) {
    List<SerpSearchRequest> requests = order.getUser().getJobRoles().stream()
        .map(role -> createRequest(order, role.getJobRole()))
        .toList();

    if (requests.isEmpty()) {
      return CompletableFuture.completedFuture(List.of());
    }

    List<CompletableFuture<List<Job>>> futures = requests.stream()
        .map(request -> jobSearchStrategy.searchAsync(
                request, executor, this::searchSync, _ -> {})
            .thenApply(AiClientResponse::getJobs))
        .toList();

    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
        .thenApply(v -> futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .toList());
  }

  // ---------------------------------------------------------------------------
  // Synchronous search
  // ---------------------------------------------------------------------------

  private AiClientResponse searchSync(JobSearchRequest request) {
    SerpSearchRequest serpRequest = (SerpSearchRequest) request;
    AiModelEntity aiModel = serpRequest.getOrder().getModel();
    UserEntity user = serpRequest.getOrder().getUser();
    log.info("Searching jobs for user {} with model {}", user.getUsername(), aiModel.getModel());
    AiClientResponse response = jobsClient.searchJobs(serpRequest);
    response.getJobs().forEach(job -> {
      job.setPromptId(serpRequest.getPromptId());
      job.setSource(aiModel.getModel());
    });
    log.info("{} found {} url's and are going to be validated", aiModel.getModel(), response.getJobs().size());
    return response;
  }

}
