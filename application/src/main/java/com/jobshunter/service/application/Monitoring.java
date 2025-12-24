package com.jobshunter.service.application;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Monitoring {

  private final Map<String, String> lastMonitorMessages = new ConcurrentHashMap<>();

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
