package com.jobshunter.service.application.scheduler;

import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.service.JobOrderDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.model.EngineSelection;
import com.jobshunter.model.OrderStatus;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.JobHuntService;
import com.jobshunter.service.application.UserCvService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobHuntScheduler {

  private final Map<String, AtomicBoolean> running = new ConcurrentHashMap<>();

  private final JobHuntService jobHuntService;

  private final UserDBService userDBService;
  private final JobOrderDBService jobOrderDBService;

  private final UserCvService userCvService;

  private final Executor executor;

  public JobHuntScheduler(
      JobHuntService jobHuntService,
      UserDBService userDBService,
      JobOrderDBService jobOrderDBService,
      UserCvService userCvService,
      @Qualifier("miscellaneousExecutor") Executor executor
  ) {
    this.jobHuntService = jobHuntService;
    this.userDBService = userDBService;
    this.jobOrderDBService = jobOrderDBService;
    this.userCvService = userCvService;
    this.executor = executor;
  }

  @Scheduled(fixedDelayString = "${jobshunter.scheduler.processOrderFrequency:5000}")
  public void processOrderAsync() {
    this.performActionAsync("processOrderAsync", this::processOrderSync);
  }

  @Scheduled(fixedDelayString = "${jobshunter.scheduler.notifyUsersFrequency:60000}")
  public void notifyUsersAsync() {
    this.performActionAsync("notifyUsersAsync", this::notifyUsersSync);
  }

  @Scheduled(fixedDelayString = "${jobshunter.scheduler.cleanupFiles:86400000}")
  public void cleanupFilesAsync() {
    this.performActionAsync("cleanupFiles", this::cleanupFilesSync);
  }

  public void processOrderSync() {
    Optional<JobOrderEntity> jobOrderOp = jobOrderDBService.getUserOldestNewOrder();
    if (jobOrderOp.isPresent()) {
      JobOrderEntity jobOrder = jobOrderOp.get();
      log.info("Start processing job order id={} for user {}", jobOrder.getId(), jobOrder.getUser().getUsername());
      jobOrder.setStatus(OrderStatus.PROCESSING);
      jobOrderDBService.saveJobOrder(jobOrder);
      try {
        SearchJobOrder searchOrder = new SearchJobOrder();
        searchOrder.setUser(jobOrder.getUser());
        searchOrder.setSearchByPrompts(jobOrder.isSearchByPrompts());
        searchOrder.setSearchCompanies(jobOrder.isSearchCompanies());
        searchOrder.setEngineSelection(new EngineSelection(jobOrder.getAiModel().getProvider(), jobOrder.getAiModel().getModel()));
        jobHuntService.searchJobsForUser(searchOrder);

        jobOrder.setStatus(OrderStatus.COMPLETED);
        jobOrderDBService.saveJobOrder(jobOrder);
        log.info("Completed processing job order id={} for user {}", jobOrder.getId(), jobOrder.getUser().getUsername());
      } catch (Exception e) {
        log.error("Error processing job order id={} for user {}: {}", jobOrder.getId(), jobOrder.getUser().getUsername(), e.getMessage(), e);
        jobOrder.setStatus(OrderStatus.FAILED);
        jobOrder.setErrorMessage(e.getMessage());
        jobOrderDBService.saveJobOrder(jobOrder);
      }
    }
  }

  public void notifyUsersSync() {
    for (var user : userDBService.getAllUsers()) {
      if (user.isNotifyWhatsapp() || user.isNotifyEmail()) {
        if (user.getTimeInterval() != null
            && user.getTimeInterval() > 0
            && user.getLastJobs() != null
            && user.getLastJobs().plus(Duration.ofMinutes(user.getTimeInterval())).isBefore(Instant.now())) {
          //log.info("Notifying user {} about new jobs found", user.getUsername());
          // TODO: implement notification logic if needed
        }
      }
    }
  }

  private void cleanupFilesSync() {
    userCvService.cleanupOldCVs();
  }

  private void performActionAsync(String actionName, Runnable runnable) {
    if (!isRunning(actionName)) {
      return;
    }
    CompletableFuture.runAsync(() -> {
      try {
        runnable.run();
      } finally {
        running.get(actionName).set(false);
      }
    }, executor);
  }

  private boolean isRunning(String taskName) {
    running.putIfAbsent(taskName, new AtomicBoolean(false));
    return running.get(taskName).compareAndSet(false, true);
  }
}
