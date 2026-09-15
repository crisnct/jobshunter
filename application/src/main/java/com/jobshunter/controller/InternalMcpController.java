package com.jobshunter.controller;

import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.RoleEntity;
import com.jobshunter.database.entities.UserContractTypeEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.entities.UserJobRoleEntity;
import com.jobshunter.database.entities.UserJobTypeEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.service.JobOrderDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.database.service.UserJobDBService;
import com.jobshunter.dto.JobOrderRequest;
import com.jobshunter.dto.SearchJobResult;
import com.jobshunter.dto.SearchJobSnapshotResponse;
import com.jobshunter.dto.SearchJobsHandle;
import com.jobshunter.dto.SearchJobsResponse;
import com.jobshunter.dto.SearchStepEvent;
import com.jobshunter.dto.UserInfoResponse;
import com.jobshunter.model.ContractType;
import com.jobshunter.model.JobType;
import com.jobshunter.model.OrderStatus;
import com.jobshunter.service.application.JobOrderProcessor;
import com.jobshunter.service.application.progress.OrderProgressStore;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/internal")
@PreAuthorize("isAuthenticated()")
public class InternalMcpController {

  private static final String SEARCH_ID_DELIMITER = ",";

  private final UserDBService userDBService;
  private final JobOrderDBService jobOrderDBService;
  private final UserJobDBService userJobDBService;
  private final OrderProgressStore orderProgressStore;
  private final JobOrderProcessor jobOrderProcessor;
  private final Executor ordersExecutor;

  public InternalMcpController(
      UserDBService userDBService,
      JobOrderDBService jobOrderDBService,
      UserJobDBService userJobDBService,
      OrderProgressStore orderProgressStore,
      JobOrderProcessor jobOrderProcessor,
      @Qualifier("ordersExecutor") Executor ordersExecutor
  ) {
    this.userDBService = userDBService;
    this.jobOrderDBService = jobOrderDBService;
    this.userJobDBService = userJobDBService;
    this.orderProgressStore = orderProgressStore;
    this.jobOrderProcessor = jobOrderProcessor;
    this.ordersExecutor = ordersExecutor;
  }

  @GetMapping("/me")
  @Transactional(readOnly = true)
  public ResponseEntity<UserInfoResponse> me(Authentication authentication) {
    UserEntity user = resolveAuthenticatedUser(authentication);
    return ResponseEntity.ok(toResponse(user));
  }

  /**
   * Enqueues one {@link JobOrderEntity} per configuration (picked up asynchronously by
   * {@code JobHuntScheduler}, same as an order created from the UI) and returns immediately with a
   * handle — it does not wait for the search to finish. Poll {@link #searchJobSnapshot} with the
   * returned {@code searchId} to track progress and retrieve results.
   */
  @PostMapping("/search_jobs")
  public ResponseEntity<SearchJobsHandle> searchJobs(
      @Valid @RequestBody List<JobOrderRequest> requests,
      Authentication authentication
  ) {
    for (JobOrderRequest request : requests) {
      if (!request.searchWithUserPrompts() && !request.searchCompanies()) {
        throw new ValidationException("At least one of searchWithUserPrompts or searchCompanies must be true");
      }
    }

    UserEntity user = resolveAuthenticatedUser(authentication);
    if (user.getCv() == null) {
      throw new ValidationException("User does not have any cv attached in his profile");
    }

    if (authentication.getDetails() instanceof Map<?, ?> details) {
      log.info("Delegated internal search for principal {} with delegated claims {}", authentication.getName(), details);
    }

    List<Long> orderIds = new ArrayList<>();
    for (JobOrderRequest request : requests) {
      log.info("Internal delegated job search for user: {}, model: {}, provider: {}",
          authentication.getName(), request.model(), request.provider().name());
      JobOrderEntity jobOrder = jobOrderDBService.createJobOrder(user, request);
      orderIds.add(jobOrder.getId());
      triggerImmediateProcessing(jobOrder.getId());
    }

    String searchId = orderIds.stream().map(String::valueOf).collect(Collectors.joining(SEARCH_ID_DELIMITER));
    return ResponseEntity.ok(new SearchJobsHandle(searchId));
  }

  /**
   * Reports current status for a search started with {@link #searchJobs}. Overall status is
   * {@code FAILED} if any of the underlying job orders failed, {@code DONE} once all of them
   * reached a terminal state successfully, otherwise {@code IN_PROGRESS}. {@code events} carries
   * chronological progress messages collected while each order is running, plus terminal outcome
   * messages.
   */
  @GetMapping("/search_jobs/{searchId}")
  @Transactional(readOnly = true)
  public ResponseEntity<SearchJobSnapshotResponse> searchJobSnapshot(
      @PathVariable("searchId") String searchId,
      Authentication authentication
  ) {
    UserEntity user = resolveAuthenticatedUser(authentication);
    List<JobOrderEntity> orders = resolveOwnedOrders(searchId, user);

    List<SearchStepEvent> events = new ArrayList<>();
    List<String> failureMessages = new ArrayList<>();
    boolean allTerminal = true;

    for (JobOrderEntity order : orders) {
      events.addAll(orderProgressStore.get(order.getId()));
      String label = "%s %s".formatted(order.getAiModel().getProvider(), order.getAiModel().getModel());
      if (order.getStatus() == OrderStatus.COMPLETED) {
        events.add(new SearchStepEvent("%s: completed".formatted(label)));
      } else if (order.getStatus() == OrderStatus.FAILED) {
        events.add(new SearchStepEvent("%s: failed".formatted(label)));
        failureMessages.add("%s: %s".formatted(label,
            StringUtils.hasText(order.getErrorMessage()) ? order.getErrorMessage() : "failed"));
      } else {
        allTerminal = false;
      }
    }

    if (!failureMessages.isEmpty()) {
      return ResponseEntity.ok(new SearchJobSnapshotResponse(
          searchId, "FAILED", events, null, String.join("; ", failureMessages)));
    }
    if (!allTerminal) {
      return ResponseEntity.ok(new SearchJobSnapshotResponse(searchId, "IN_PROGRESS", events, null, null));
    }

    List<SearchJobResult> jobsFound = orders.stream()
        .flatMap(order -> userJobDBService.getUserJobs(user.getUsername(), order.getId()).stream())
        .distinct()
        .sorted(Comparator.comparing(
            (UserJobEntity job) -> job.getScore() == null ? Integer.MIN_VALUE : job.getScore()).reversed())
        .map(job -> new SearchJobResult(job.getUrl(), job.getAiModel() != null ? job.getAiModel().getModel() : null))
        .toList();
    return ResponseEntity.ok(new SearchJobSnapshotResponse(searchId, "DONE", events, new SearchJobsResponse(jobsFound), null));
  }

  /**
   * Resolves a {@code searchId} (comma-joined job order ids, in the order returned by
   * {@link #searchJobs}) to its owned {@link JobOrderEntity} list. Any parse failure, missing
   * order, or order belonging to a different user surfaces as a plain 404 - never distinguished
   * from "not found" - so a caller can't use this endpoint to probe for other users' order ids.
   */
  private List<JobOrderEntity> resolveOwnedOrders(String searchId, UserEntity user) {
    List<Long> orderIds;
    try {
      orderIds = Arrays.stream(searchId.split(SEARCH_ID_DELIMITER))
          .map(String::trim)
          .filter(StringUtils::hasText)
          .map(Long::parseLong)
          .toList();
    } catch (NumberFormatException e) {
      orderIds = List.of();
    }
    if (orderIds.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Search not found.");
    }

    Map<Long, JobOrderEntity> byId = jobOrderDBService.getJobOrders(orderIds).stream()
        .collect(Collectors.toMap(JobOrderEntity::getId, order -> order));

    List<JobOrderEntity> ordered = new ArrayList<>(orderIds.size());
    for (Long orderId : orderIds) {
      JobOrderEntity order = byId.get(orderId);
      if (order == null || !order.getUser().getId().equals(user.getId())) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Search not found.");
      }
      ordered.add(order);
    }
    return ordered;
  }

  private UserEntity resolveAuthenticatedUser(Authentication authentication) {
    String principal = authentication != null ? authentication.getName() : null;
    if (!StringUtils.hasText(principal)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authenticated principal.");
    }

    Optional<UserEntity> userOptional = userDBService.getUserCompleteInfo(principal);
    Optional<UserEntity> emailLookupOptional = userDBService.getUserByEmail(principal);
    return userOptional.or(() -> emailLookupOptional).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Authenticated principal is not registered in Jobshunter."
        )
    );
  }

  private UserInfoResponse toResponse(UserEntity user) {
    List<String> roles = user.getRoles().stream()
        .map(RoleEntity::getName)
        .toList();
    List<UserPromptEntity> prompts = user.getPrompts();
    List<String> jobRoles = user.getJobRoles().stream()
        .map(UserJobRoleEntity::getJobRole)
        .toList();
    List<JobType> jobTypes = user.getJobTypes().stream()
        .map(UserJobTypeEntity::getJobType)
        .toList();
    List<ContractType> contractTypes = user.getContractTypes().stream()
        .map(UserContractTypeEntity::getContractType)
        .toList();

    return new UserInfoResponse(
        user.getUsername(),
        user.getEmail(),
        user.getPhoneNumber(),
        user.isNotifyWhatsapp(),
        user.isNotifyEmail(),
        user.isEmailVerified(),
        user.getVerificationToken(),
        user.getCv() != null ? user.getCv().getFilename() : "",
        formatDateTime(user.getNotifiedAt()),
        prompts.stream().map(UserPromptEntity::getPrompt).toList(),
        formatDateTime(user.getCreatedAt()),
        roles,
        user.getCity(),
        user.getCountry(),
        user.getJobDomain(),
        jobRoles,
        jobTypes,
        user.getRelocation(),
        contractTypes
    );
  }

  private String formatDateTime(Instant dateTime) {
    return dateTime != null ? dateTime.toString() : null;
  }

  private void triggerImmediateProcessing(Long orderId) {
    CompletableFuture.runAsync(() -> {
      try {
        if (jobOrderDBService.tryAcquireSpecificOrder(orderId)) {
          jobOrderProcessor.process(orderId);
        } else {
          log.debug("Order {} already acquired for processing by another worker", orderId);
        }
      } catch (Exception e) {
        log.error("Immediate processing failed for order {}: {}", orderId, e.getMessage(), e);
      }
    }, ordersExecutor);
  }
}
