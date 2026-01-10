package com.jobshunter.service.application.scheduler;

import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserSessionEntity;
import com.jobshunter.database.service.JobOrderDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.database.service.UserSessionDBService;
import com.jobshunter.model.OrderStatus;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.JobHuntService;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.clients.IpInfo;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

  private final Executor ordersExecutor;

  private final Executor notificationsExecutor;

  private final Executor maintenanceExecutor;

  private final IpInfo ipInfoClient;

  private final UserSessionDBService userSessionDBService;

  public JobHuntScheduler(
      JobHuntService jobHuntService,
      UserDBService userDBService,
      JobOrderDBService jobOrderDBService,
      UserCvService userCvService,
      UserSessionDBService userSessionDBService,
      IpInfo ipInfoClient,
      @Qualifier("ordersExecutor") Executor ordersExecutor,
      @Qualifier("notificationsExecutor") Executor notificationsExecutor,
      @Qualifier("maintenanceExecutor") Executor maintenanceExecutor
  ) {
    this.jobHuntService = jobHuntService;
    this.ipInfoClient = ipInfoClient;
    this.userDBService = userDBService;
    this.userSessionDBService = userSessionDBService;
    this.jobOrderDBService = jobOrderDBService;
    this.userCvService = userCvService;
    this.ordersExecutor = ordersExecutor;
    this.notificationsExecutor = notificationsExecutor;
    this.maintenanceExecutor = maintenanceExecutor;
  }

  @Scheduled(fixedDelayString = "${jobshunter.scheduler.processOrderFrequency:5000}")
  public void processOrderAsync() {
    Optional<Long> jobId = jobOrderDBService.acquireJobId();
    if (jobId.isPresent()) {
      this.performActionAsync("processOrderAsync", () -> processOrderSync(jobId.get()), ordersExecutor);
    }
  }

  @Scheduled(fixedDelayString = "${jobshunter.scheduler.notifyUsersFrequency:60000}")
  public void notifyUsersAsync() {
    final List<UserEntity> usersToNotify = new ArrayList<>();
    for (var user : userDBService.getAllUsers()) {
      if (user.isNotifyWhatsapp() || user.isNotifyEmail()) {
        if (user.getLastJobs() != null && user.getLastJobs().plus(Duration.ofDays(1)).isBefore(Instant.now())) {
          usersToNotify.add(user);
        }
      }
    }
    if (!usersToNotify.isEmpty()) {
      this.performActionAsync("notifyUsersAsync", () -> notifyUsersSync(usersToNotify), notificationsExecutor);
    }
  }

  @Scheduled(fixedDelayString = "${jobshunter.scheduler.cleanupFiles:86400000}")
  public void cleanupFilesAsync() {
    this.performActionAsync("cleanupFiles", this::cleanupFilesSync, maintenanceExecutor);
  }

  public void processOrderSync(Long jobId) {
    JobOrderEntity jobOrder = jobOrderDBService.getJobOrder(jobId);
    log.info("Start processing job order id={} for user {}", jobOrder.getId(), jobOrder.getUser().getUsername());
    try {
      UserSessionEntity session = userSessionDBService.findByUser(jobOrder.getUser());
      SearchJobOrder order = new SearchJobOrder(jobOrder);
      order.setIpInfo(ipInfoClient.getIpDetailInfo(session.getIpAddress()));
      jobHuntService.searchJobsForUser(order);
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

  public void notifyUsersSync(List<UserEntity> usersToNotify) {
    //log.info("Notifying user {} about new jobs found", user.getUsername());
    // TODO: implement notification logic if needed
  }

  private void cleanupFilesSync() {
    userCvService.cleanupOldCVs();
  }

  private void performActionAsync(String actionName, Runnable runnable, Executor executor) {
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
