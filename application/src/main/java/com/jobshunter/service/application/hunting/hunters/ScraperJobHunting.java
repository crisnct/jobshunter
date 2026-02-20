package com.jobshunter.service.application.hunting.hunters;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.dto.JobSearchRequest;
import com.jobshunter.dto.ScraperSearchRequest;
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
public final class ScraperJobHunting implements JobHunting, JobByPromptHunting {

  private final AiJobsClient<ScraperSearchRequest> jobsClient;
  private final Executor executor;
  private final AiDefaultStrategy jobSearchStrategy;

  public ScraperJobHunting(
      @Qualifier("JobsClientScraper") AiJobsClient<ScraperSearchRequest> serpClient,
      @Qualifier("scraperExecutor") Executor executor,
      AiDefaultStrategy strategy
  ) {
    this.jobsClient = serpClient;
    this.executor = executor;
    this.jobSearchStrategy = strategy;
  }

  private ScraperSearchRequest createRequest(SearchJobOrder order, String prompt) {
    return ScraperSearchRequest.builder(order).userPrompt(prompt).build();
  }

  @Override
  public CompletableFuture<List<Job>> searchJobsAsync(SearchJobOrder order) {
    List<ScraperSearchRequest> requests = order.getUser().getJobRoles().stream()
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

  @Override
  public EngineType getEngineType() {
    return EngineType.SCRAPER;
  }

  // ---------------------------------------------------------------------------
  // Synchronous search
  // ---------------------------------------------------------------------------

  private AiClientResponse searchSync(JobSearchRequest request) {
    ScraperSearchRequest serpRequest = (ScraperSearchRequest) request;
    AiModelEntity aiModel = serpRequest.getOrder().getModel();
    UserEntity user = serpRequest.getOrder().getUser();
    log.info("Searching jobs for user {} with model {}", user.getUsername(), aiModel.getModel());
    AiClientResponse response = jobsClient.searchJobs(serpRequest);
    response.getJobs().forEach(job -> {
      job.setSource(aiModel.getModel());
    });
    log.info("{} found {} url's and are going to be validated", aiModel.getModel(), response.getJobs().size());
    return response;
  }

}
