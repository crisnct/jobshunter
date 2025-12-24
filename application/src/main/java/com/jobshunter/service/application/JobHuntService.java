package com.jobshunter.service.application;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.EngineType;
import com.jobshunter.dto.Job;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.dto.SearchJobOrder;
import com.jobshunter.service.application.hunting.GeminiJobHunting;
import com.jobshunter.service.application.hunting.GptJobHunting;
import com.jobshunter.service.application.hunting.JobHunting;
import com.jobshunter.service.application.hunting.SerpJobHunting;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import com.jobshunter.service.application.notifiers.WhatsappNotifierService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobHuntService {

  private final ApplicationProperties properties;

  private final WhatsappNotifierService whatsappNotifierService;

  private final EmailNotifierService emailNotifierService;

  private final UserDataService userDataService;

  private final SerpJobHunting serpJobHunting;

  private final GptJobHunting gptJobHunting;

  private final GeminiJobHunting geminiJobHunting;

  private final JobScoring jobScoring;

  private final JobsValidator jobsValidator;

  public JobHuntService(
      ApplicationProperties properties,
      WhatsappNotifierService whatsappNotifierService,
      EmailNotifierService emailNotifierService,
      UserDataService userDataService,
      SerpJobHunting serpJobHunting,
      GptJobHunting gptJobHunting,
      GeminiJobHunting geminiJobHunting,
      JobScoring jobScoring,
      JobsValidator jobsValidator
  ) {
    this.properties = properties;
    this.whatsappNotifierService = whatsappNotifierService;
    this.emailNotifierService = emailNotifierService;
    this.userDataService = userDataService;
    this.serpJobHunting = serpJobHunting;
    this.gptJobHunting = gptJobHunting;
    this.geminiJobHunting = geminiJobHunting;
    this.jobScoring = jobScoring;
    this.jobsValidator = jobsValidator;
  }

  public void scheduledRun() {
    log.info("Starts scheduled job hunt...");
    for (var user : userDataService.getAllUsers()) {
      if (user.isNotifyWhatsapp() || user.isNotifyEmail()) {
        if (user.getTimeInterval() != null
            && user.getTimeInterval() > 0
            && user.getLastJobs() != null
            && user.getLastJobs().plusMinutes(user.getTimeInterval()).isBefore(LocalDateTime.now())) {
          log.info("Start searching jobs for {} ", user.getUsername());
          this.searchJobsForUser(new SearchJobOrder(
              user,
              List.of(EngineType.GPT4),
              properties.getJobsHunter().getIterationPerUser()
          ));
        }
      }
    }
    log.info("Stop scheduled job hunt.");
  }

  public JobHuntResponse searchJobsForUser(SearchJobOrder order) {
    UserEntity user = order.user();
    final JobsSynchronizer jobsSync =
        new JobsSynchronizer(userDataService.getExistingJobUrlsForUser(user.getUsername()));

    List<CompletableFuture<Void>> enginesFutures = new ArrayList<>();
    this.searchJobs("SERP", serpJobHunting, jobsSync, order, enginesFutures);
    this.searchJobs("GPT", gptJobHunting, jobsSync, order, enginesFutures);
    this.searchJobs("GEMINI", geminiJobHunting, jobsSync, order, enginesFutures);

    CompletableFuture.allOf(enginesFutures.toArray(CompletableFuture[]::new)).join();

    //Follow redirects and validate url's
    List<Job> validatedJobs = jobsValidator.validateJobs(jobsSync.getJobs());

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
    } else if (user.isNotifyEmail() || user.isNotifyWhatsapp()) {
      userDataService.updateUser(user, jobs);
      log.info("Found {} jobs for {} ", jobs.size(), user.getEmail());
      jobs.forEach(System.out::println);

      if (user.isNotifyWhatsapp()) {
        whatsappNotifierService.send(jobs, user);
      }
      if (user.isNotifyEmail()) {
        emailNotifierService.sendUsingTemplate(jobs, user);
      }
    }

    return jobHuntResponse;
  }

  private void searchJobs(String enginePrefix, JobHunting jobHunting, JobsSynchronizer jobsSync, SearchJobOrder order,
      List<CompletableFuture<Void>> enginesFutures) {
    List<EngineType> enginesFiltered = order.engines().stream().filter(p -> p.name().startsWith(enginePrefix)).toList();
    if (!enginesFiltered.isEmpty()) {
      SearchJobOrder orderClone = new SearchJobOrder(
          order.user(),
          enginesFiltered,
          order.iterations()
      );
      enginesFutures.add(jobHunting.searchJobs(jobsSync, orderClone));
    }
  }


}
