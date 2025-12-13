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

  // TODO to be enable after we have at least 5 real users in the database
  //@Scheduled(fixedDelayString = "${jobshunter.scheduler.frequency:3600000}")
  public void scheduledRun() throws InterruptedException {
    jobHuntService.scheduledRun();
  }
}
