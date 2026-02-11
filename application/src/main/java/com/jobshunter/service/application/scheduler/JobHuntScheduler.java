package com.jobshunter.service.application.scheduler;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.service.JobOrderDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.database.service.UserJobDBService;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.OrderStatus;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.security.filters.CorrelationIdFilter;
import com.jobshunter.service.application.JobHuntService;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.application.hunting.CountryIsoCode;
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

  private final JobHuntService jobHuntService;

  private final UserDBService userDBService;

  private final UserJobDBService userJobDBService;

  private final JobOrderDBService jobOrderDBService;

  private final UserCvService userCvService;

  private final ApplicationProperties properties;

  private final Executor ordersExecutor;

  private final Executor notificationsExecutor;

  private final Executor maintenanceExecutor;

  private final WhatsappNotifierService whatsappNotifierService;

  private final EmailNotifierService emailNotifierService;

  private final CountryIsoCode countryIsoCode;

  private final Environment environment;

  public JobHuntScheduler(
      JobHuntService jobHuntService,
      UserDBService userDBService,
      JobOrderDBService jobOrderDBService,
      UserCvService userCvService,
      Environment env,
      ApplicationProperties properties,
      UserJobDBService userJobDBService,
      CountryIsoCode countryIsoCode,
      WhatsappNotifierService whatsappNotifierService,
      EmailNotifierService emailNotifierService,
      @Qualifier("ordersExecutor") Executor ordersExecutor,
      @Qualifier("notificationsExecutor") Executor notificationsExecutor,
      @Qualifier("maintenanceExecutor") Executor maintenanceExecutor
  ) {
    this.jobHuntService = jobHuntService;
    this.whatsappNotifierService = whatsappNotifierService;
    this.emailNotifierService = emailNotifierService;
    this.userDBService = userDBService;
    this.jobOrderDBService = jobOrderDBService;
    this.userCvService = userCvService;
    this.userJobDBService = userJobDBService;
    this.countryIsoCode = countryIsoCode;
    this.ordersExecutor = ordersExecutor;
    this.notificationsExecutor = notificationsExecutor;
    this.maintenanceExecutor = maintenanceExecutor;
    this.properties = properties;
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
    // Generate correlation ID for scheduled task
    String correlationId = UUID.randomUUID().toString();
    MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, correlationId);
    try {
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

        boolean isEnableOneRealEngine = (properties.getGemini().isEnabled() || properties.getGpt().isEnabled() || properties.getSerp().isEnabled());
        final List<String> ignoredURLs;
        if (isEnableOneRealEngine) {
          ignoredURLs = userJobDBService.getUserJobs(username).stream().map(UserJobEntity::getUrl).toList();
        } else {
          ignoredURLs = List.of();
        }
        SearchJobOrder order = new SearchJobOrder(jobOrder, user, ignoredURLs);
        order.setCountryISOcode(countryIsoCode.getCode(user.getCountry()));

        jobHuntService.searchJobsForUser(order);
        jobOrderDBService.changeStatus(jobOrder.getId(), OrderStatus.COMPLETED, null);
        log.info("Completed processing job order id={} for user {}", jobOrder.getId(), jobOrder.getUser().getUsername());
      } catch (Exception e) {
        log.error("Error processing job order id={} for user {}: {}", jobOrder.getId(), jobOrder.getUser().getUsername(), e.getMessage(), e);
        jobOrderDBService.changeStatus(jobOrder.getId(), OrderStatus.FAILED, e.getMessage());
      }
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
