package com.jobshunter.service.application;

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
