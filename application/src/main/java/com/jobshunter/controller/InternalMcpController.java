package com.jobshunter.controller;

import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.RoleEntity;
import com.jobshunter.database.entities.UserContractTypeEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobRoleEntity;
import com.jobshunter.database.entities.UserJobTypeEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.service.JobOrderDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.dto.JobOrderRequest;
import com.jobshunter.dto.SearchJobResult;
import com.jobshunter.dto.SearchJobsResponse;
import com.jobshunter.dto.UserInfoResponse;
import com.jobshunter.model.ContractType;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobType;
import com.jobshunter.model.OrderStatus;
import com.jobshunter.service.application.JobOrderProcessor;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InternalMcpController {

  private final UserDBService userDBService;
  private final JobOrderDBService jobOrderDBService;
  private final JobOrderProcessor jobOrderProcessor;

  @GetMapping("/me")
  @Transactional(readOnly = true)
  public ResponseEntity<UserInfoResponse> me(Authentication authentication) {
    UserEntity user = resolveAuthenticatedUser(authentication);
    return ResponseEntity.ok(toResponse(user));
  }

  @PostMapping("/search_jobs")
  public ResponseEntity<SearchJobsResponse> searchJobs(
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

    List<Job> jobsFound = new ArrayList<>();
    for (JobOrderRequest request : requests) {
      log.info("Internal delegated job search for user: {}, model: {}, provider: {}",
          authentication.getName(), request.model(), request.provider().name());
      JobOrderEntity jobOrder = jobOrderDBService.createJobOrder(user, request, OrderStatus.PROCESSING);
      JobHuntResponse huntResponse = jobOrderProcessor.process(jobOrder.getId());
      jobsFound.addAll(huntResponse.jobsFound());
    }

    List<SearchJobResult> uniqueJobs = jobsFound.stream()
        .distinct()
        .sorted(Comparator.comparing(Job::getScore).reversed())
        .map(job -> new SearchJobResult(job.getUrl(), job.getSource()))
        .toList();
    return ResponseEntity.ok(new SearchJobsResponse(uniqueJobs));
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
}
