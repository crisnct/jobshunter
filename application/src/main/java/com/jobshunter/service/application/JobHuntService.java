package com.jobshunter.service.application;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.model.EngineSelection;
import com.jobshunter.model.EngineTier;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.hunting.HuntingOrchestrator;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import com.jobshunter.service.application.notifiers.WhatsappNotifierService;
import com.jobshunter.service.application.processors.JobScoring;
import com.jobshunter.service.application.processors.JobsStateMachine;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class JobHuntService {

  private final WhatsappNotifierService whatsappNotifierService;

  private final EmailNotifierService emailNotifierService;

  private final UserDataService userDataService;

  private final JobScoring jobScoring;

  private final HuntingOrchestrator huntingOrchestrator;

  private final JobsStateMachine jobsStateMachine;

  private final ApplicationProperties properties;

  public void scheduledRun() throws IOException {
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
    final List<String> existingURLs;
    boolean isEnableOneRealEngine = (properties.getGemini().isEnabled() || properties.getGpt().isEnabled() || properties.getSerpApi().isEnabled());
    if (isEnableOneRealEngine) {
      existingURLs = userDataService.getExistingJobUrlsForUser(user.getUsername());
    } else {
      existingURLs = new ArrayList<>();
    }

    String resumeFileIdCleanup = null;
    try {
      String resumeFileId = jobScoring.uploadUserCv(user.getCv());
      resumeFileIdCleanup = resumeFileId;

      CompletableFuture<List<Job>> futureJobs = huntingOrchestrator.startHunting(order, existingURLs);
      List<JobContext> result = jobsStateMachine.processAsync(futureJobs, user, resumeFileId);

      List<Job> validatedJobs = result.stream()
          .filter(JobContext::isAccepted)
          .map(JobContext::getJob)
          .toList();

      JobHuntResponse jobHuntResponse = new JobHuntResponse(validatedJobs.stream()
          .sorted(Comparator.comparing(Job::getScore).reversed())
          .toList());

      List<Job> jobs = jobHuntResponse.jobsFound();
      if (!jobs.isEmpty()) {
        if (isEnableOneRealEngine) {
          Map<Long, List<Job>> jobsByPromptId = jobs.stream().collect(Collectors.groupingBy(Job::getPromptId));
          this.userDataService.saveJobsToDB(jobsByPromptId, user);
        }
        if (user.isNotifyWhatsapp()) {
          whatsappNotifierService.send(jobs, user);
        }
        if (user.isNotifyEmail()) {
          emailNotifierService.sendUsingTemplate(jobs, user);
        }
      }
      return jobHuntResponse;
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new RuntimeException("Unexpected error about creating file on local storage " + e.getMessage(), e);
    } finally {
      if (resumeFileIdCleanup != null) {
        jobScoring.cleanup(resumeFileIdCleanup);
      }
    }
  }

}
