package com.jobshunter.service.application;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Monitoring {

  private final Map<String, String> lastMonitorMessages = new ConcurrentHashMap<>();

  private final ThreadPoolExecutor gptSearchExecutor;

  private final ThreadPoolExecutor geminiSearchExecutor;

  private final ThreadPoolExecutor serpApiExecutor;

  private final ThreadPoolExecutor browseURLExecutor;

  public Monitoring(
      @Qualifier("gptSearchExecutor") ThreadPoolExecutor gptSearchExecutor,
      @Qualifier("geminiSearchExecutor") ThreadPoolExecutor geminiSearchExecutor,
      @Qualifier("serpApiExecutor") ThreadPoolExecutor serpApiExecutor,
      @Qualifier("browseURLExecutor") ThreadPoolExecutor browseURLExecutor
  ) {
    this.gptSearchExecutor = gptSearchExecutor;
    this.geminiSearchExecutor = geminiSearchExecutor;
    this.serpApiExecutor = serpApiExecutor;
    this.browseURLExecutor = browseURLExecutor;
  }

  @Scheduled(fixedDelay = 10000)
  public void monitorExecutors() {
    monitorExecutor("Gpt executor", gptSearchExecutor);
    monitorExecutor("Gemini executor", geminiSearchExecutor);
    monitorExecutor("Serp executor", serpApiExecutor);
    monitorExecutor("Miscellaneous executor", browseURLExecutor);
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
