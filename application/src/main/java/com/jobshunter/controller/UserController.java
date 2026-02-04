package com.jobshunter.controller;

import com.jobshunter.database.entities.RoleEntity;
import com.jobshunter.database.entities.UserContractTypeEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.entities.UserJobRoleEntity;
import com.jobshunter.database.entities.UserJobTypeEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.service.AuthDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.database.service.UserJobDBService;
import com.jobshunter.dto.ChangePasswordRequest;
import com.jobshunter.dto.UserInfoResponse;
import com.jobshunter.dto.UserJobResponse;
import com.jobshunter.dto.UserUpdateRequest;
import com.jobshunter.model.ContractType;
import com.jobshunter.model.EngineCategory;
import com.jobshunter.model.JobType;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserController {

  private final UserDBService userDBService;
  private final UserJobDBService userJobDBService;
  private final AuthDBService authDBService;
  private final EmailNotifierService emailService;
  private final UserCvService userCvService;

  @GetMapping("/me")
  @Transactional(readOnly = true)
  public ResponseEntity<UserInfoResponse> me(Authentication authentication) {
    log.info("Get user info for {}", authentication.getName());
    //noinspection OptionalGetWithoutIsPresent
    return userDBService.getUserCompleteInfo(authentication.getName())
        .map(user -> ResponseEntity.ok(toResponse(user)))
        .get();
  }

  @GetMapping("/all")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<UserInfoResponse>> getAllUsers() {
    List<UserInfoResponse> users = userDBService.getAllUsers().stream()
        .map(this::toResponse)
        .toList();
    return ResponseEntity.ok(users);
  }

  @GetMapping("/jobs")
  public ResponseEntity<?> getUserJobs(
      Authentication authentication,
      @RequestParam(value = "orderId", required = false)
      Long orderId
  ) {
    String username = authentication.getName();
    List<UserJobResponse> jobs = userJobDBService.getUserJobs(username, orderId).stream()
        .map(this::toUserJobResponse)
        .toList();
    return ResponseEntity.ok(jobs);
  }

  @PatchMapping("/password")
  public ResponseEntity<?> changePassword(
      @Valid
      @RequestBody
      ChangePasswordRequest request,

      Authentication authentication
  ) {
    UserEntity user = authDBService.changePassword(authentication.getName(), request);
    emailService.sendVerificationToken(user);
    return ResponseEntity.ok(Map.of("message", "Check for email with token"));
  }

  private UserInfoResponse toResponse(UserEntity user) {
    List<String> roles = user.getRoles().stream()
        .map(RoleEntity::getName)
        .toList();
    List<UserPromptEntity> prompts = user.getPrompts();

    // Initialize lazy collections
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
        prompts.stream()
            .map(UserPromptEntity::getPrompt)
            .toList(),
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

  private UserJobResponse toUserJobResponse(UserJobEntity userJob) {
    return new UserJobResponse(
        userJob.getUrl(),
        formatDateTime(userJob.getCreatedAt())
    );
  }

  private String formatDateTime(Instant dateTime) {
    return dateTime != null ? dateTime.toString() : null;
  }

  @PatchMapping("/approve")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> approveUser(
      @RequestParam("username")
      @NotBlank
      @Size(max = 255)
      String username,

      @RequestParam(value = "rejectReason", required = false)
      @Size(max = 255)
      String rejectReason
  ) {
    //noinspection OptionalGetWithoutIsPresent
    UserEntity user = userDBService.getUser(username).get();

    String responseMessage;
    if (Strings.isEmpty(rejectReason)) {
      if (user.isApproved()) {
        return ResponseEntity.badRequest().body(Map.of("error", "User already approved"));
      }
      responseMessage = "User approved";
      user.setApproved(true);
      userDBService.updateUser(user);
      emailService.accountApproved(user);
    } else {
      responseMessage = "User rejected with reason: " + rejectReason;
      emailService.accountRejected(user, rejectReason);
    }
    return ResponseEntity.ok(Map.of("message", responseMessage));
  }

  @DeleteMapping("/delete")
  public ResponseEntity<?> deleteAccount(Authentication authentication) {
    userCvService.deleteUserCv(authentication.getName());
    userDBService.deleteUserByUsername(authentication.getName());
    return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
  }

  @Transactional
  @PutMapping("/update")
  public ResponseEntity<?> updateUser(
      @Valid @RequestBody UserUpdateRequest request
  ) {
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    UserEntity user = userDBService.getUser(request.username()).get();
    user.setPhoneNumber(request.phoneNumber());
    user.setNotifyWhatsapp(request.notifyWhatsapp());
    user.setNotifyEmail(request.notifyEmail());
    user.setCity(request.city());
    user.setCountry(request.country());
    user.setJobDomain(request.jobDomain());
    user.setRelocation(request.relocation());

    // Update job roles, job types, and contract types through service
    if (request.jobRoles() != null) {
      userDBService.updateUserJobRoles(user, request.jobRoles());
    }
    if (request.jobTypes() != null) {
      userDBService.updateUserJobTypes(user, request.jobTypes());
    }
    if (request.contractTypes() != null) {
      userDBService.updateUserContractTypes(user, request.contractTypes());
    }

    List<Long> promptsToDelete = new ArrayList<>(user.getPrompts().stream().map(UserPromptEntity::getId).toList());

    if (request.aiPrompts() != null) {
      request.aiPrompts().forEach(aiPrompt -> {
        UserPromptEntity prompt = userDBService.updatePrompt(user, EngineCategory.AI, aiPrompt.id(), aiPrompt.prompt());
        promptsToDelete.remove(prompt.getId());
      });
    }

    userDBService.deleteUserPrompts(promptsToDelete);
    userDBService.updateUser(user);
    return ResponseEntity.ok(Map.of("user", user.getUsername(), "message", "User updated successfully"));
  }

}

