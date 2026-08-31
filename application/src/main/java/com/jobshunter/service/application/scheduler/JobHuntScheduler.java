package com.jobshunter.service.application.scheduler;

import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.JobOrderDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.database.service.UserJobDBService;
import com.jobshunter.model.Job;
import com.jobshunter.security.filters.CorrelationIdFilter;
import com.jobshunter.service.application.JobOrderProcessor;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import com.jobshunter.service.application.notifiers.WhatsappNotifierService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "jobshunter.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class JobHuntScheduler {

  private final Map<String, AtomicBoolean> running = new ConcurrentHashMap<>();

  private final JobOrderProcessor jobOrderProcessor;

  private final UserDBService userDBService;

  private final UserJobDBService userJobDBService;

  private final JobOrderDBService jobOrderDBService;

  private final UserCvService userCvService;

  private final Executor ordersExecutor;

  private final Executor notificationsExecutor;

  private final Executor maintenanceExecutor;

  private final WhatsappNotifierService whatsappNotifierService;

  private final EmailNotifierService emailNotifierService;

  private final Environment environment;

  public JobHuntScheduler(
      JobOrderProcessor jobOrderProcessor,
      UserDBService userDBService,
      JobOrderDBService jobOrderDBService,
      UserCvService userCvService,
      Environment env,
      UserJobDBService userJobDBService,
      WhatsappNotifierService whatsappNotifierService,
      EmailNotifierService emailNotifierService,
      @Qualifier("ordersExecutor") Executor ordersExecutor,
      @Qualifier("notificationsExecutor") Executor notificationsExecutor,
      @Qualifier("maintenanceExecutor") Executor maintenanceExecutor
  ) {
    this.jobOrderProcessor = jobOrderProcessor;
    this.whatsappNotifierService = whatsappNotifierService;
    this.emailNotifierService = emailNotifierService;
    this.userDBService = userDBService;
    this.jobOrderDBService = jobOrderDBService;
    this.userCvService = userCvService;
    this.userJobDBService = userJobDBService;
    this.ordersExecutor = ordersExecutor;
    this.notificationsExecutor = notificationsExecutor;
    this.maintenanceExecutor = maintenanceExecutor;
    this.environment = env;
  }

  /**
   * Processes orders concurrently. Each invocation submits a task to the ordersExecutor, allowing multiple orders to be processed in parallel (up to
   * the executor's thread pool size). The FOR UPDATE SKIP LOCKED in acquireJobId() ensures each order is processed only once.
   */
  @Scheduled(fixedDelayString = "${jobshunter.scheduler.processOrderFrequency:5000}")
  public void processOrderAsync() {
    CompletableFuture.runAsync(this::processOrderSync, ordersExecutor);
  }

  @Scheduled(fixedDelayString = "${jobshunter.scheduler.notifyUsersFrequency:180000}")
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
    String correlationId = UUID.randomUUID().toString();
    MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, correlationId);
    try {
      jobOrderProcessor.process(jobIdOp.get());
    } catch (Exception e) {
      log.error("Error processing job order id={}: {}", jobIdOp.get(), e.getMessage(), e);
    } finally {
      MDC.clear();
    }
  }

  public void notifyUsersSync() {
    for (var user : userDBService.getAllUsers()) {
      if (user.isNotifyWhatsapp() || user.isNotifyEmail()) {
        if (user.getNotifiedAt() == null || user.getNotifiedAt().plus(Duration.ofHours(12)).isBefore(Instant.now())) {
          List<JobOrderEntity> orders = jobOrderDBService.getCompletedOrdersNotNotified(user.getId());
          if (!orders.isEmpty()) {
            notifyUser(user, orders);
          }
        }
      }
    }
  }

  private void notifyUser(UserEntity user, List<JobOrderEntity> orders) {
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
      log.info("Notifying user {} about new jobs found...", user.getUsername());
      if (user.isNotifyWhatsapp()) {
        whatsappNotifierService.send(jobs, user);
      }
      if (user.isNotifyEmail()) {
        emailNotifierService.sendUsingTemplate(jobs, user);
      }
      if (environment.matchesProfiles("prod")) {
        user.setNotifiedAt(Instant.now());
        userDBService.updateUser(user);
      }
      log.info("Notified user {} about {} jobs found", user.getUsername(), jobs.size());
      jobs.forEach(p -> log.info(p.getUrl()));
    }
    if (environment.matchesProfiles("prod")) {
      jobOrderDBService.setNotified(orders);
    }
  }

  private void cleanupFilesSync() {
    userCvService.cleanupOldCVs();
  }

  private void performActionAsync(String actionName, Runnable runnable, Executor executor) {
    if (!isRunning(actionName)) {
      return;
    }
    // Generate correlation ID for scheduled task so it propagates to all child threads
    String correlationId = UUID.randomUUID().toString();
    MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, correlationId);
    try {
      CompletableFuture.runAsync(() -> {
        try {
          runnable.run();
        } finally {
          running.get(actionName).set(false);
        }
      }, executor);
    } finally {
      MDC.clear(); // Clear on scheduler thread after submission
    }
  }

  private boolean isRunning(String taskName) {
    running.putIfAbsent(taskName, new AtomicBoolean(false));
    return running.get(taskName).compareAndSet(false, true);
  }
}
