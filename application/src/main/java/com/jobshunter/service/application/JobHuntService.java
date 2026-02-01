package com.jobshunter.service.application;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.UserJobDBService;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.hunting.HuntingOrchestrator;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import com.jobshunter.service.application.notifiers.WhatsappNotifierService;
import com.jobshunter.service.application.processors.JobsStateMachine;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class JobHuntService {

  private final WhatsappNotifierService whatsappNotifierService;

  private final EmailNotifierService emailNotifierService;

  private final UserJobDBService userJobDBService;

  private final HuntingOrchestrator huntingOrchestrator;

  private final JobsStateMachine jobsStateMachine;

  private final ApplicationProperties properties;

  public JobHuntResponse searchJobsForUser(SearchJobOrder order) {
    UserEntity user = order.getUser();
    CompletableFuture<List<Job>> futureJobs = huntingOrchestrator.startHunting(order);
    List<Job> result = jobsStateMachine.processAsync(futureJobs, user, order)
        .join()
        .stream()
        .filter(ctx -> !ctx.isFailed() && ctx.isValidatedSuccessfully())
        .map(JobContext::getJob)
        .toList();

    JobHuntResponse jobHuntResponse = new JobHuntResponse(result.stream()
        .sorted(Comparator.comparing(Job::getScore).reversed())
        .toList());

    List<Job> jobs = jobHuntResponse.jobsFound();
    if (!jobs.isEmpty()) {
      if ((properties.getGemini().isEnabled() || properties.getGpt().isEnabled() || properties.getSerp().isEnabled())) {
        this.userJobDBService.updateUserWithJobs(user, order, jobs);
      }
      if (user.isNotifyWhatsapp()) {
        whatsappNotifierService.send(jobs, user);
      }
      if (user.isNotifyEmail()) {
        emailNotifierService.sendUsingTemplate(jobs, user);
      }
    }
    return jobHuntResponse;
  }

}
