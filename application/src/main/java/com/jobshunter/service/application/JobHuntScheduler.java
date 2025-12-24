package com.jobshunter.service.application;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
  private ThreadPoolExecutor gptSearchExecutor;

  @Autowired
  @Qualifier("geminiSearchExecutor")
  private ThreadPoolExecutor geminiSearchExecutor;

  @Autowired
  @Qualifier("serpApiExecutor")
  private ThreadPoolExecutor serpApiExecutor;

  @Autowired
  @Qualifier("jobsValidatorExecutor")
  private ThreadPoolExecutor jobsValidatorExecutor;

  @Autowired
  private JobHuntService jobHuntService;

  private final Map<String, String> lastMonitorMessages = new ConcurrentHashMap<>();

  // TODO to be enable after we have at least 5 real users in the database
  // @Scheduled(fixedDelayString = "${jobshunter.scheduler.frequency:3600000}")
  public void scheduledRun() throws InterruptedException {
    jobHuntService.scheduledRun();
  }

  @Scheduled(fixedDelay = 10000)
  public void monitorExecutors() {
    monitorExecutor("GPT executor", gptSearchExecutor);
    monitorExecutor("GEMINI executor", geminiSearchExecutor);
    monitorExecutor("SERP executor", serpApiExecutor);
    monitorExecutor("URL validator executor", jobsValidatorExecutor);
  }

  private void monitorExecutor(String executorName, ThreadPoolExecutor executor) {
    String newMessage = String.format(
        "%s - active: %d, queued: %d, completed: %d",
        executorName,
        executor.getActiveCount(),
        executor.getQueue().size(),
        executor.getCompletedTaskCount()
    );

    String lastMessage = lastMonitorMessages.get(executorName);
    if (!newMessage.equals(lastMessage)) {
      log.info(newMessage);
      lastMonitorMessages.put(executorName, newMessage);
    }
  }
}
