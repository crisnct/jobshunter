package com.jobshunter.controller;

import com.jobshunter.service.LimitedVirtualThreadExecutor;
import com.jobshunter.service.UrlAffinityExecutor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
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
@PreAuthorize("isAuthenticated()")
public class MonitoringController {

  private static final String TABLE_HEADER = """
      NAME                  | QUEUED | ACTIVE | COMPLETED | ERRORS | LIMIT
      ----------------------+--------+--------+-----------+--------+------
      """;
  private static final String NAME_COLUMN_SIZE = "21";

  private final LimitedVirtualThreadExecutor gptSearchExecutor;
  private final LimitedVirtualThreadExecutor grokSearchExecutor;
  private final LimitedVirtualThreadExecutor geminiSearchExecutor;
  private final LimitedVirtualThreadExecutor serpExecutor;
  private final LimitedVirtualThreadExecutor urlFetchPlaywrightExecutor;
  private final LimitedVirtualThreadExecutor jobProcessingExecutor;
  private final LimitedVirtualThreadExecutor urlFetchRestClientExecutor;
  private final LimitedVirtualThreadExecutor ordersExecutor;
  private final LimitedVirtualThreadExecutor notificationsExecutor;

  private final TaskScheduler scheduler;
  private final UrlAffinityExecutor urlAffinityExecutor;

  public MonitoringController(
      @Qualifier("gptSearchExecutor") LimitedVirtualThreadExecutor gptSearchExecutor,
      @Qualifier("grokSearchExecutor") LimitedVirtualThreadExecutor grokSearchExecutor,
      @Qualifier("geminiSearchExecutor") LimitedVirtualThreadExecutor geminiSearchExecutor,
      @Qualifier("serpExecutor") LimitedVirtualThreadExecutor serpExecutor,
      @Qualifier("urlFetchPlaywrightExecutor") LimitedVirtualThreadExecutor urlFetchPlaywrightExecutor,
      @Qualifier("urlFetchRestClientExecutor") LimitedVirtualThreadExecutor urlFetchRestClientExecutor,
      @Qualifier("jobProcessingExecutor") LimitedVirtualThreadExecutor jobProcessingExecutor,
      @Qualifier("ordersExecutor") LimitedVirtualThreadExecutor ordersExecutor,
      @Qualifier("notificationsExecutor") LimitedVirtualThreadExecutor notificationsExecutor,

      TaskScheduler scheduler,
      UrlAffinityExecutor urlAffinityExecutor
  ) {
    this.gptSearchExecutor = gptSearchExecutor;
    this.grokSearchExecutor = grokSearchExecutor;
    this.geminiSearchExecutor = geminiSearchExecutor;
    this.urlFetchPlaywrightExecutor = urlFetchPlaywrightExecutor;
    this.serpExecutor = serpExecutor;
    this.jobProcessingExecutor = jobProcessingExecutor;
    this.ordersExecutor = ordersExecutor;
    this.notificationsExecutor = notificationsExecutor;
    this.urlFetchRestClientExecutor = urlFetchRestClientExecutor;
    this.scheduler = scheduler;
    this.urlAffinityExecutor = urlAffinityExecutor;
  }

  @GetMapping("/executors")
  public ResponseEntity<String> getExecutorsStatus() {
    log.info("Fetching status of executors");

    String table = TABLE_HEADER + formatExecutorRow("GPT", gptSearchExecutor) + '\n'
        + formatExecutorRow("Grok", grokSearchExecutor) + '\n'
        + formatExecutorRow("Gemini", geminiSearchExecutor) + '\n'
        + formatExecutorRow("SERP", serpExecutor) + '\n'
        + formatExecutorRow("Job Processing", jobProcessingExecutor) + '\n'
        + formatExecutorRow("Orders", ordersExecutor) + '\n'
        + formatExecutorRow("Notifications", notificationsExecutor) + '\n'
        + formatExecutorRow("URL-Fetch-REST Client", urlFetchRestClientExecutor) + '\n'
        + formatExecutorRow("URL-Fetch-Playwright", urlFetchPlaywrightExecutor) + '\n'
        + formatSchedulerRow(
        "Scheduler",
        ((ThreadPoolTaskScheduler) scheduler).getScheduledThreadPoolExecutor()
    )
        + '\n'
        + "\nURL Affinity executors: "
        + urlAffinityExecutor.getAllExecutors().size();

    return ResponseEntity.ok(table);
  }

  private String formatExecutorRow(String name, LimitedVirtualThreadExecutor executor) {
    return String.format(
        "%-" + NAME_COLUMN_SIZE + "s | %6d | %6d | %9d | %6d | %5d",
        name,
        executor.getQueue(),
        executor.getActiveThreads(),
        executor.getCompletedTaskSuccessCount(),
        executor.getTasksWithErrors(),
        executor.getMaxThreads()
    );
  }

  private String formatSchedulerRow(String name, ScheduledThreadPoolExecutor executor) {
    return String.format(
        "%-" + NAME_COLUMN_SIZE + "s | %6s | %6d | %9d | %6s | %5s",
        name,
        "-",
        executor.getActiveCount(),
        executor.getCompletedTaskCount(),
        "-",
        "-"
    );
  }

}
