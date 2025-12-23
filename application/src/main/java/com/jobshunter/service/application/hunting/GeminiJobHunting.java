package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.EngineType;
import com.jobshunter.dto.Job;
import com.jobshunter.dto.SearchJobOrder;
import com.jobshunter.dto.geminiRequest.GeminiJobSearchRequest;
import com.jobshunter.service.application.JobsSynchronizer;
import com.jobshunter.service.clients.AiJobsClient;
import java.util.ArrayList;
import java.util.Base64;
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
public non-sealed class GeminiJobHunting implements JobHunting {

  @Autowired
  @Qualifier("geminiSearchExecutor")
  private Executor geminiSearchExecutor;

  @Autowired
  @Qualifier("EconomyJobsClientGemini")
  private AiJobsClient<GeminiJobSearchRequest, List<Job>> geminiEconomy;

  @Autowired
  private UserDataService userDataService;

  @Override
  public CompletableFuture<Void> searchJobs(JobsSynchronizer jobsSync, SearchJobOrder order) {
    UserEntity user = order.user();
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    String userCVBase64 = Base64.getEncoder().encodeToString(user.getCv().getCv());
    int delayCounter = 0;
    for (int i = 0; i < order.iterations(); i++) {
      for (EngineType engine : order.engines()) {
        for (UserPromptEntity prompt : user.getPrompts()) {
          if (prompt.getEngine() == engine) {
            Executor delayedExecutor = CompletableFuture.delayedExecutor(
                (long) delayCounter++ * order.iterations(),
                TimeUnit.MILLISECONDS,
                geminiSearchExecutor
            );
            GeminiJobSearchRequest request = new GeminiJobSearchRequest(user.getUsername(), prompt, userCVBase64, engine);
            futures.add(CompletableFuture.runAsync(() -> geminiSearch(jobsSync, request), delayedExecutor));
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

  private void geminiSearch(
      JobsSynchronizer jobsSync,
      GeminiJobSearchRequest request
  ) {
    log.info("Searching jobs for user {} with gpt model {}", request.getUsername(), request.getEngine());
    List<Job> jobsFound = switch (request.getEngine()) {
      case GEMINI_2_5_FLASH -> throw new IllegalStateException("Not implemented yet");
      case GEMINI_2_5_FLASH_LITE -> geminiEconomy.searchJobs(request);
      case GEMINI_2_5_PRO -> throw new IllegalStateException("Not implemented yet");
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GPT model");
    };
    jobsSync.addJobs(jobsFound);
    userDataService.incrementPromptJobsFound(request.getPrompt().getId(), jobsFound.size());
    log.info("{} found {} jobs for {}. Are going to be validated.", request.getEngine().name(), jobsFound.size(), request.getUsername());
  }

}
