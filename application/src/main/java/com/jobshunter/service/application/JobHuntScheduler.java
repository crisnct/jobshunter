package com.jobshunter.service.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

//@Component
//@Slf4j
//@Profile("prod")
public class JobHuntScheduler {

//  @Autowired
//  private JobHuntService jobHuntService;

  // TODO to be enable after we have at least 5 real users in the database
  // @Scheduled(fixedDelayString = "${jobshunter.scheduler.frequency:3600000}")
  public void scheduledRun() throws InterruptedException {
    //jobHuntService.scheduledRun();
  }

}
