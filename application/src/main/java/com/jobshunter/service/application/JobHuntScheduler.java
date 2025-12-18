package com.jobshunter.service.application;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Profile("prod")
public class JobHuntScheduler {

  @Autowired
  @Qualifier("gptSearchExecutor")
  private Executor gptSearchExecutor;

  @Autowired
  private JobHuntService jobHuntService;

  // TODO to be enable after we have at least 5 real users in the database
  //@Scheduled(fixedDelayString = "${jobshunter.scheduler.frequency:3600000}")
  public void scheduledRun() throws InterruptedException {
    jobHuntService.scheduledRun();
  }

  @Scheduled(fixedDelay = 10000)
  public void monitorGptExecutor() {
    if (gptSearchExecutor instanceof ThreadPoolExecutor exec) {
      log.info("GPT executor - active: {}, queued: {}, completed: {}",
          exec.getActiveCount(), exec.getQueue().size(), exec.getCompletedTaskCount());
    }
  }

}
