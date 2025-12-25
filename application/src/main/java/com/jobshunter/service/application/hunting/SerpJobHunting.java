package com.jobshunter.service.application.hunting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.model.EngineSelection;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.dto.serpRequest.SearchWithSerpRequest;
import com.jobshunter.dto.serpResponse.SerpApiJobHit;
import com.jobshunter.dto.serpResponse.SerpApiJobsResult;
import com.jobshunter.service.application.JobsSynchronizer;
import com.jobshunter.service.clients.GptJobScoreCalculatorClient;
import com.jobshunter.service.clients.SerpApiClient;
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

  private final SerpApiClient<SearchWithSerpRequest, SerpApiJobsResult> serpApiClient;

  private final GptJobScoreCalculatorClient scoreCalculator;

  private final Executor serpApiExecutor;

  private final JsonMapper mapper;

  public SerpJobHunting(
      SerpApiClient<SearchWithSerpRequest, SerpApiJobsResult> serpApiClient,
      GptJobScoreCalculatorClient scoreCalculator,
      @Qualifier("serpApiExecutor") Executor serpApiExecutor,
      JsonMapper mapper
  ) {
    this.serpApiClient = serpApiClient;
    this.scoreCalculator = scoreCalculator;
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
            futures.add(CompletableFuture.runAsync(() -> searchWithSerpAPi(jobsSync, prompt, user, selection), delayedExecutor));
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

  private void searchWithSerpAPi(
      JobsSynchronizer jobsSync,
      UserPromptEntity prompt,
      UserEntity user,
      EngineSelection selection
  ) {
    try {
      log.info("Searching jobs for user {} with serp api", user.getUsername());
      SearchWithSerpRequest request = mapper.readValue(prompt.getPrompt(), SearchWithSerpRequest.class);
      SerpApiJobsResult serpApiResult = serpApiClient.searchJobs(request);

      final List<Job> jobs = new ArrayList<>();
      for (SerpApiJobHit job : serpApiResult.jobs()) {
        String jobDescription = job.description() + "\n" + job.highlights();
        int score = scoreCalculator.computeScore(jobDescription, user.getCv().getGptFileId());
        jobs.add(new Job(score, job.applyLinks().getFirst(), "serp"));
      }
      jobsSync.addJobs(jobs, EngineType.SERP, selection.tier(), prompt.getId());
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
  }
}
