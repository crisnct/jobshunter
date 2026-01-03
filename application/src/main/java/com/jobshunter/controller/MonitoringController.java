package com.jobshunter.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/monitoring")
@PreAuthorize("hasRole('ADMIN')")
public class MonitoringController {

  private final ThreadPoolExecutor gptSearchExecutor;

  private final ThreadPoolExecutor grokSearchExecutor;

  private final ThreadPoolExecutor geminiSearchExecutor;

  private final ThreadPoolExecutor serpExecutor;

  private final ThreadPoolExecutor miscellaneousExecutor;

  private final TaskScheduler scheduler;

  public MonitoringController(
      @Qualifier("gptSearchExecutor") ThreadPoolExecutor gptSearchExecutor,
      @Qualifier("grokSearchExecutor") ThreadPoolExecutor grokSearchExecutor,
      @Qualifier("geminiSearchExecutor") ThreadPoolExecutor geminiSearchExecutor,
      @Qualifier("serpExecutor") ThreadPoolExecutor serpExecutor,
      @Qualifier("miscellaneousExecutor") ThreadPoolExecutor miscellaneousExecutor,
      TaskScheduler scheduler
  ) {
    this.gptSearchExecutor = gptSearchExecutor;
    this.grokSearchExecutor = grokSearchExecutor;
    this.geminiSearchExecutor = geminiSearchExecutor;
    this.serpExecutor = serpExecutor;
    this.miscellaneousExecutor = miscellaneousExecutor;
    this.scheduler = scheduler;
  }

  @GetMapping("/executors")
  public ResponseEntity<Map<String, String>> getExecutorsStatus() {
    log.info("Fetching status of executors");
    Map<String, String> engineModelsMap = new LinkedHashMap<>();
    engineModelsMap.put("Gpt", formatMessage(gptSearchExecutor));
    engineModelsMap.put("Grok", formatMessage(grokSearchExecutor));
    engineModelsMap.put("Gemini", formatMessage(geminiSearchExecutor));
    engineModelsMap.put("Serp", formatMessage(serpExecutor));
    engineModelsMap.put("Miscellaneous", formatMessage(miscellaneousExecutor));
    engineModelsMap.put("Scheduler", formatMessage(((ThreadPoolTaskScheduler) scheduler).getScheduledThreadPoolExecutor()));
    return ResponseEntity.ok(engineModelsMap);
  }

  private String formatMessage(ThreadPoolExecutor executor) {
    return String.format(
        "active: %d, queued: %d, completed: %d",
        executor.getActiveCount(),
        executor.getQueue().size(),
        executor.getCompletedTaskCount()
    );
  }

}
