package com.jobshunter.service.application;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.model.EngineSelection;
import com.jobshunter.model.EngineTier;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.hunting.GeminiJobHunting;
import com.jobshunter.service.application.hunting.GptJobHunting;
import com.jobshunter.service.application.hunting.JobHunting;
import com.jobshunter.service.application.hunting.SerpJobHunting;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import com.jobshunter.service.application.notifiers.WhatsappNotifierService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobHuntService {

  private final WhatsappNotifierService whatsappNotifierService;

  private final EmailNotifierService emailNotifierService;

  private final UserDataService userDataService;

  private final SerpJobHunting serpJobHunting;

  private final GptJobHunting gptJobHunting;

  private final GeminiJobHunting geminiJobHunting;

  private final JobScoring jobScoring;

  private final JobsValidator jobsValidator;

  public void scheduledRun() {
    log.info("Starts scheduled job hunt...");
    for (var user : userDataService.getAllUsers()) {
      if (user.isNotifyWhatsapp() || user.isNotifyEmail()) {
        if (user.getTimeInterval() != null
            && user.getTimeInterval() > 0
            && user.getLastJobs() != null
            && user.getLastJobs().plusMinutes(user.getTimeInterval()).isBefore(LocalDateTime.now())) {
          log.info("Start searching jobs for {} ", user.getUsername());
          this.searchJobsForUser(
              new SearchJobOrder(
                  user,
                  List.of(new EngineSelection(EngineType.GPT, EngineTier.ECONOMY))
              )
          );
        }
      }
    }
    log.info("Stop scheduled job hunt.");
  }

  public JobHuntResponse searchJobsForUser(SearchJobOrder order) {
    UserEntity user = order.user();
    final JobsSynchronizer synchronizer =
        new JobsSynchronizer(userDataService.getExistingJobUrlsForUser(user.getUsername()));

    List<CompletableFuture<Void>> enginesFutures = new ArrayList<>();
    this.searchJobs(EngineType.SERP, serpJobHunting, synchronizer, order, enginesFutures);
    this.searchJobs(EngineType.GPT, gptJobHunting, synchronizer, order, enginesFutures);
    this.searchJobs(EngineType.GEMINI, geminiJobHunting, synchronizer, order, enginesFutures);

    CompletableFuture.allOf(enginesFutures.toArray(CompletableFuture[]::new)).join();

    //Follow redirects and validate url's
    List<Job> jobsFromHunters = synchronizer.getJobs().values().stream()
        .flatMap((Function<List<Job>, Stream<Job>>) Collection::stream)
        .toList();
    List<Job> validatedJobs = jobsValidator.validateJobs(jobsFromHunters);

    //TODO scoring
    for (Job job : validatedJobs) {
      jobScoring.calculateScore(job, order.user());
    }

    JobHuntResponse jobHuntResponse = new JobHuntResponse(validatedJobs.stream()
        .sorted(Comparator.comparing(Job::getScore).reversed())
        .toList());

    List<Job> jobs = jobHuntResponse.jobsFound();
    if (jobs.isEmpty()) {
      log.info("No jobs found for user {} ", user.getUsername());
    } else {
      this.userDataService.saveJobsToDB(synchronizer, user, jobs);
      if (user.isNotifyWhatsapp()) {
        whatsappNotifierService.send(jobs, user);
      }
      if (user.isNotifyEmail()) {
        emailNotifierService.sendUsingTemplate(jobs, user);
      }
    }

    return jobHuntResponse;
  }

  private void searchJobs(
      EngineType engineType,
      JobHunting jobHunting,
      JobsSynchronizer jobsSync,
      SearchJobOrder order,
      List<CompletableFuture<Void>> enginesFutures
  ) {
    List<EngineSelection> enginesFiltered = order.engines().stream()
        .filter(selection -> selection.type() == engineType)
        .toList();
    if (enginesFiltered.isEmpty()) {
      return;
    }
    SearchJobOrder orderClone = new SearchJobOrder(order.user(), enginesFiltered);
    enginesFutures.add(jobHunting.searchJobs(jobsSync, orderClone));
  }


}
