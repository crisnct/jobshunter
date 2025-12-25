package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.model.EngineSelection;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.GptJobSearchRequest;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public non-sealed class GptJobHunting implements JobHunting {

  private final AiJobsClient<GptJobSearchRequest, List<Job>> gptEconomy;

  private final AiJobsClient<GptJobSearchRequest, List<Job>> gptPremium;

  private final Executor gptSearchExecutor;

  public GptJobHunting(
      @Qualifier("EconomyJobsClientGPT") AiJobsClient<GptJobSearchRequest, List<Job>> gptEconomy,
      @Qualifier("PremiumJobsClientGPT") AiJobsClient<GptJobSearchRequest, List<Job>> gptPremium,
      @Qualifier("gptSearchExecutor") Executor gptSearchExecutor
  ) {
    this.gptEconomy = gptEconomy;
    this.gptPremium = gptPremium;
    this.gptSearchExecutor = gptSearchExecutor;
  }

  @Override
  public CompletableFuture<Void> searchJobs(JobsSynchronizer jobsSync, SearchJobOrder order) {
    UserEntity user = order.user();
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    int delayCounter = 0;
    String gptFileId = order.user().getCv().getGptFileId();
    for (int i = 0; i < order.iterations(); i++) {
      for (EngineSelection selection : order.engines()) {
        for (UserPromptEntity prompt : user.getPrompts()) {
          if (prompt.getEngine() == EngineType.GPT) {
            Executor delayedExecutor = CompletableFuture.delayedExecutor(
                (long) delayCounter++ * order.iterations(),
                TimeUnit.MILLISECONDS,
                gptSearchExecutor
            );

            GptJobSearchRequest request = new GptJobSearchRequest(
                user.getUsername(),
                prompt,
                selection.type(),
                selection.tier(),
                gptFileId
            );
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
    log.info("Searching jobs for user {} with gpt model {}-{}", request.getUsername(), request.getEngineType(),
        request.getEngineTier());
    List<Job> jobsFound = switch (request.getEngineTier()) {
      case ECONOMY -> gptEconomy.searchJobs(request);
      case PREMIUM -> gptPremium.searchJobs(request);
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GPT tier");
    };
    jobsSync.addJobs(jobsFound, request.getEngineType(), request.getEngineTier(), request.getPrompt().getId());
  }

}
