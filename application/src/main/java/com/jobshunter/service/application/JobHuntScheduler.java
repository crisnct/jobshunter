package com.jobshunter.service.application;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class JobHuntScheduler {

  private final JobHuntService jobHuntService;

  public JobHuntScheduler(JobHuntService jobHuntService) {
    this.jobHuntService = jobHuntService;
  }

  @Scheduled(fixedDelayString = "${jobshunter.scheduler.frequency:3600000}")
  public void scheduledRun() throws InterruptedException {
    jobHuntService.scheduledRun();
  }
}
