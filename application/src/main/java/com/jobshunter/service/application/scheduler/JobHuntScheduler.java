package com.jobshunter.service.application.scheduler;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.entities.UserSessionEntity;
import com.jobshunter.database.service.JobOrderDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.database.service.UserJobDBService;
import com.jobshunter.database.service.UserSessionDBService;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.OrderStatus;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.JobHuntService;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import com.jobshunter.service.application.notifiers.WhatsappNotifierService;
import com.jobshunter.service.clients.IpInfo;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "jobshunter.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class JobHuntScheduler {

  private final Map<String, AtomicBoolean> running = new ConcurrentHashMap<>();

  private final JobHuntService jobHuntService;

  private final UserDBService userDBService;

  private final UserJobDBService userJobDBService;

  private final JobOrderDBService jobOrderDBService;

  private final UserCvService userCvService;

  private final ApplicationProperties properties;

  private final Executor ordersExecutor;

  private final Executor notificationsExecutor;

  private final Executor maintenanceExecutor;

  private final IpInfo ipInfoClient;

  private final WhatsappNotifierService whatsappNotifierService;

  private final EmailNotifierService emailNotifierService;

  private final UserSessionDBService userSessionDBService;

  public JobHuntScheduler(
      JobHuntService jobHuntService,
      UserDBService userDBService,
      JobOrderDBService jobOrderDBService,
      UserCvService userCvService,
      ApplicationProperties properties,
      UserJobDBService userJobDBService,
      UserSessionDBService userSessionDBService,
      IpInfo ipInfoClient,
      WhatsappNotifierService whatsappNotifierService,
      EmailNotifierService emailNotifierService,
      @Qualifier("ordersExecutor") Executor ordersExecutor,
      @Qualifier("notificationsExecutor") Executor notificationsExecutor,
      @Qualifier("maintenanceExecutor") Executor maintenanceExecutor
  ) {
    this.jobHuntService = jobHuntService;
    this.whatsappNotifierService = whatsappNotifierService;
    this.emailNotifierService = emailNotifierService;
    this.ipInfoClient = ipInfoClient;
    this.userDBService = userDBService;
    this.userSessionDBService = userSessionDBService;
    this.jobOrderDBService = jobOrderDBService;
    this.userCvService = userCvService;
    this.userJobDBService = userJobDBService;
    this.ordersExecutor = ordersExecutor;
    this.notificationsExecutor = notificationsExecutor;
    this.maintenanceExecutor = maintenanceExecutor;
    this.properties = properties;
  }

  @Scheduled(fixedDelayString = "${jobshunter.scheduler.processOrderFrequency:5000}")
  public void processOrderAsync() {
    this.performActionAsync("processOrderAsync", this::processOrderSync, ordersExecutor);
  }

  @Scheduled(fixedDelayString = "${jobshunter.scheduler.notifyUsersFrequency:60000}")
  public void notifyUsersAsync() {
    this.performActionAsync("notifyUsersAsync", this::notifyUsersSync, notificationsExecutor);
  }

  @Scheduled(fixedDelayString = "${jobshunter.scheduler.cleanupFiles:86400000}", initialDelayString = "PT12H")
  public void cleanupFilesAsync() {
    this.performActionAsync("cleanupFiles", this::cleanupFilesSync, maintenanceExecutor);
  }

  public void processOrderSync() {
    Optional<Long> jobIdOp = jobOrderDBService.acquireJobId();
    if (jobIdOp.isEmpty()) {
      return;
    }
    JobOrderEntity jobOrder = jobOrderDBService.getJobOrder(jobIdOp.get());
    String username = jobOrder.getUser().getUsername();
    log.info("Start processing job order id={} for user {}", jobOrder.getId(), jobOrder.getUser().getUsername());
    try {
      for (EngineType type : EngineType.values()) {
        if (type.isAiProvider()) {
          userCvService.refreshUserCvIfNeeded(jobOrder.getUser(), type);
        }
      }

      UserEntity user = userDBService.getUserCompleteInfo(username).orElseThrow();
      UserSessionEntity session = userSessionDBService.findByUser(user);

      boolean isEnableOneRealEngine = (properties.getGemini().isEnabled() || properties.getGpt().isEnabled() || properties.getSerp().isEnabled());
      final List<String> ignoredURLs;
      if (isEnableOneRealEngine) {
        ignoredURLs = userJobDBService.getUserJobs(username).stream().map(UserJobEntity::getUrl).toList();
      } else {
        ignoredURLs = List.of();
      }
      SearchJobOrder order = new SearchJobOrder(jobOrder, user, ignoredURLs);
      order.setIpInfo(ipInfoClient.getIpDetailInfo(session.getIpAddress()));
      jobHuntService.searchJobsForUser(order);
      jobOrderDBService.changeStatus(jobOrder.getId(), OrderStatus.COMPLETED, null);
      log.info("Completed processing job order id={} for user {}", jobOrder.getId(), jobOrder.getUser().getUsername());
    } catch (Exception e) {
      log.error("Error processing job order id={} for user {}: {}", jobOrder.getId(), jobOrder.getUser().getUsername(), e.getMessage(), e);
      jobOrderDBService.changeStatus(jobOrder.getId(), OrderStatus.FAILED, e.getMessage());
    }
  }

  public void notifyUsersSync() {
    for (var user : userDBService.getAllUsers()) {
      if (user.isNotifyWhatsapp() || user.isNotifyEmail()) {
        if (user.getLastJobs() != null && user.getLastJobs().plus(Duration.ofDays(1)).isBefore(Instant.now())) {
          List<JobOrderEntity> orders = jobOrderDBService.getCompletedOrdersNotNotified(user.getId());
          if (!orders.isEmpty()) {
            notifyUser(user, orders);
          }
        }
      }
    }
  }

  private void notifyUser(UserEntity user, List<JobOrderEntity> orders) {
    log.info("Notifying user {} about new jobs found...", user.getUsername());
    List<Job> jobs = orders.stream()
        .flatMap(order -> userJobDBService.getUserJobs(user.getUsername(), order.getId()).stream())
        .map(userJob -> {
          Job job = new Job(userJob.getUrl());
          job.setSource(userJob.getAiModel().getModel());
          if (userJob.getScore() != null) {
            job.setScore(userJob.getScore());
          }
          return job;
        })
        .sorted((o1, o2) -> -Integer.compare(o1.getScore(), o2.getScore()))
        .toList();
    if (!jobs.isEmpty()) {
      if (user.isNotifyWhatsapp()) {
        whatsappNotifierService.send(jobs, user);
      }
      if (user.isNotifyEmail()) {
        emailNotifierService.sendUsingTemplate(jobs, user);
      }
      user.setLastJobs(Instant.now());
      userDBService.updateUser(user);
      log.info("Notified user {} about {} jobs found", user.getUsername(), jobs.size());
    }
    jobOrderDBService.setNotified(orders);
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
