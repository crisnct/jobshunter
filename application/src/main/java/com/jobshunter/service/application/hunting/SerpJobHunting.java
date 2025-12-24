package com.jobshunter.service.application.hunting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.EngineType;
import com.jobshunter.dto.Job;
import com.jobshunter.dto.SearchJobOrder;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public non-sealed class SerpJobHunting implements JobHunting {

  @Autowired
  private SerpApiClient<SearchWithSerpRequest, SerpApiJobsResult> serpApiClient;

  @Autowired
  private GptJobScoreCalculatorClient scoreCalculator;

  @Autowired
  @Qualifier("serpApiExecutor")
  private Executor serpApiExecutor;

  @Autowired
  private JsonMapper mapper;

  @Override
  public CompletableFuture<Void> searchJobs(JobsSynchronizer jobsSync, SearchJobOrder order) {
    return CompletableFuture.runAsync(() -> searchWithSerpAPi(jobsSync, order.user()), serpApiExecutor);
  }

  private void searchWithSerpAPi(JobsSynchronizer jobsSync, UserEntity user) {
    try {
      log.info("Searching jobs for user {} with serp api", user.getUsername());
      String serpPayload = user.getPrompts().stream()
          .filter(p -> p.getEngine() == EngineType.SERP)
          .map(UserPromptEntity::getPrompt)
          .findFirst()
          .orElse(null);
      if (Strings.isEmpty(serpPayload)) {
        log.info("Skip serp api search for {} because serp prompt is missing", user.getUsername());
        return;
      }
      SearchWithSerpRequest request = mapper.readValue(serpPayload, SearchWithSerpRequest.class);
      SerpApiJobsResult serpApiResult = serpApiClient.searchJobs(request);

      final List<Job> jobs = new ArrayList<>();
      for (SerpApiJobHit job : serpApiResult.jobs()) {
        String jobDescription = job.description() + "\n" + job.highlights();
        int score = scoreCalculator.computeScore(jobDescription, user.getCv().getGptFileId());
        jobs.add(new Job(score, job.applyLinks().getFirst(), "Google"));
      }
      jobsSync.addJobs(jobs, EngineType.SERP);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
  }
}
