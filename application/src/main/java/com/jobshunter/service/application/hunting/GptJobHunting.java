package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.EngineType;
import com.jobshunter.dto.Job;
import com.jobshunter.dto.SearchJobOrder;
import com.jobshunter.dto.gptRequest.GptJobSearchRequest;
import com.jobshunter.service.application.JobsSynchronizer;
import com.jobshunter.service.clients.AiJobsClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public non-sealed class GptJobHunting implements JobHunting {

  @Autowired
  @Qualifier("EconomyJobsClientGPT")
  private AiJobsClient<GptJobSearchRequest, List<Job>> gptEconomy;

  @Autowired
  @Qualifier("PremiumJobsClientGPT")
  private AiJobsClient<GptJobSearchRequest, List<Job>> gptPremium;

  @Autowired
  private UserDataService userDataService;

  @Autowired
  @Qualifier("gptSearchExecutor")
  private Executor gptSearchExecutor;

  @Override
  public CompletableFuture<Void> searchJobs(JobsSynchronizer jobsSync, SearchJobOrder order) {
    UserEntity user = order.user();
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    int delayCounter = 0;
    for (int i = 0; i < order.iterations(); i++) {
      for (EngineType engine : order.engines()) {
        for (UserPromptEntity prompt : user.getPrompts()) {
          if (prompt.getEngine() == engine) {
            Executor delayedExecutor = CompletableFuture.delayedExecutor(
                (long) delayCounter++ * order.iterations(),
                TimeUnit.MILLISECONDS,
                gptSearchExecutor
            );
            GptJobSearchRequest request = new GptJobSearchRequest(user.getUsername(), prompt, engine, order.user().getCv().getGptFileId());
            futures.add(CompletableFuture.runAsync(() -> gptSearch(jobsSync, request), delayedExecutor));
          }
        }
      }
    }

    // Combine all async iteration futures into one
    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
        .exceptionally(ex -> {
          log.error("GPT search failed for {}", user.getUsername(), ex);
          return null;
        });
  }

  private void gptSearch(
      JobsSynchronizer jobsSync,
      GptJobSearchRequest request
  ) {
    log.info("Searching jobs for user {} with gpt model {}", request.getUsername(), request.getEngine());
    List<Job> jobsFound = switch (request.getEngine()) {
      case GPT4 -> gptEconomy.searchJobs(request);
      case GPT5 -> gptPremium.searchJobs(request);
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GPT model");
    };
    jobsSync.addJobs(jobsFound, request.getEngine());
    userDataService.incrementPromptJobsFound(request.getPrompt().getId(), jobsFound.size());
    log.info("Found {} jobs for {}. Are going to be validated.", jobsFound.size(), request.getUsername());
  }

}
