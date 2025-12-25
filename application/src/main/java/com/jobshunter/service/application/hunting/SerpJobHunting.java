package com.jobshunter.service.application.hunting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.serpRequest.SearchWithSerpRequest;
import com.jobshunter.model.EngineSelection;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.JobsSynchronizer;
import com.jobshunter.service.clients.AiJobsClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public non-sealed class SerpJobHunting implements JobHunting {

  private final AiJobsClient<SearchWithSerpRequest, List<Job>> serpApiClient;

  private final Executor serpApiExecutor;

  private final JsonMapper mapper;

  public SerpJobHunting(
      @Qualifier("EconomyJobsClientSerp") AiJobsClient<SearchWithSerpRequest, List<Job>> serpApiClient,
      @Qualifier("serpApiExecutor") Executor serpApiExecutor,
      JsonMapper mapper
  ) {
    this.serpApiClient = serpApiClient;
    this.serpApiExecutor = serpApiExecutor;
    this.mapper = mapper;
  }

  @Override
  public CompletableFuture<Void> searchJobs(JobsSynchronizer jobsSync, SearchJobOrder order) {
    UserEntity user = order.user();
    long delayCounter = 0;
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (int i = 0; i < order.iterations(); i++) {
      for (EngineSelection selection : order.engines()) {
        for (UserPromptEntity prompt : user.getPrompts()) {
          if (prompt.getEngine() == EngineType.SERP && Strings.isNotBlank(prompt.getPrompt())) {
            Executor delayedExecutor = CompletableFuture.delayedExecutor(
                //TODO implement rate limiter for sepa and read rate limiter and use it here
                delayCounter++ * 10000,
                TimeUnit.MILLISECONDS,
                serpApiExecutor
            );
            try {
              SearchWithSerpRequest request = mapper.readValue(prompt.getPrompt(), SearchWithSerpRequest.class);
              request.setEngineTier(selection.tier());
              request.setEngineType(selection.type());
              request.setUsername(user.getUsername());
              request.setPrompt(prompt);

              futures.add(CompletableFuture.runAsync(() -> searchWithSerpAPi(jobsSync, request), delayedExecutor));
            } catch (JsonProcessingException e) {
              throw new RuntimeException(e);
            }
          }
        }
      }
    }

    // Combine all async iteration futures into one
    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
        .exceptionally(ex -> {
          log.error("SERP search failed for {}", user.getUsername(), ex);
          return null;
        });
  }

  private void searchWithSerpAPi(JobsSynchronizer jobsSync, SearchWithSerpRequest request) {
    log.info("Searching jobs for user {} with serp api", request.getUsername());
    List<Job> jobsFound = switch (request.getEngineTier()) {
      case ECONOMY -> serpApiClient.searchJobs(request);
      case PREMIUM -> throw new IllegalStateException("Not implemented yet");
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid engine tier");
    };
    jobsSync.addJobs(jobsFound, EngineType.SERP, request.getEngineTier(), request.getPrompt().getId());
  }
}
