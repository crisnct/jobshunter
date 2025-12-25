package com.jobshunter.controller;

import com.jobshunter.database.entities.RoleEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.service.AuthService;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.ChangePasswordRequest;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.dto.SearchJobsRequest;
import com.jobshunter.dto.UserInfoResponse;
import com.jobshunter.dto.UserJobResponse;
import com.jobshunter.dto.UserPromptRequest;
import com.jobshunter.service.application.JobHuntService;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

  @Autowired
  private UserDataService userDataService;

  @Autowired
  private AuthService authService;

  @Autowired
  private JobHuntService jobHuntService;

  @Autowired
  private EmailNotifierService emailService;

  @Autowired
  private UserCvService userCvService;

  @GetMapping("/me")
  public ResponseEntity<?> me(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
    }
    log.info("Get user info for {}",authentication.getName());
    return userDataService.getUser(authentication.getName())
        .<ResponseEntity<?>>map(user -> ResponseEntity.ok(toResponse(user)))
        .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "User not found")));
  }

  @PostMapping("/search")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> search(
      @Valid
      @RequestBody
      SearchJobsRequest request
  ) {
    return userDataService.getUser(request.username())
        .map(user -> new SearchJobOrder(user, new ArrayList<>(request.engines()), request.iterations()))
        .map(order -> jobHuntService.searchJobsForUser(order))
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.ok(new JobHuntResponse(Collections.emptyList())));
  }

  @GetMapping("/all")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<UserInfoResponse>> getAllUsers() {
    List<UserInfoResponse> users = userDataService.getAllUsers().stream()
        .map(this::toResponse)
        .toList();
    return ResponseEntity.ok(users);
  }

  @GetMapping("/jobs")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> getUserJobs(
      @RequestParam("username")
      @NotBlank @Size(max = 255)
      String username
  ) {
    List<UserJobResponse> jobs = userDataService.getUserJobs(username).stream()
        .map(this::toUserJobResponse)
        .toList();
    return ResponseEntity.ok(jobs);
  }

  @PatchMapping("/time-interval")
  public ResponseEntity<?> setTimeInterval(
      @RequestParam("minutes")
      @NotNull
      @Positive
      Integer minutes,

      Authentication authentication
  ) {
    String username = authentication != null ? authentication.getName() : null;
    if (username == null) {
      return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
    }
    userDataService.getUser(username).ifPresent(user -> {
      user.setTimeInterval(minutes);
      userDataService.updateUser(user);
    });
    return ResponseEntity.ok(Map.of("message", "Time interval updated"));
  }

  @PatchMapping("/prompt")
  public ResponseEntity<?> setPrompt(
      @Valid
      @RequestBody
      UserPromptRequest request,

      Authentication authentication
  ) {
    String username = authentication != null ? authentication.getName() : null;
    if (username == null) {
      return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
    }
    userDataService.getUser(username).ifPresent(user -> {
      userDataService.addPrompt(request.id(), user, request.engine(), request.prompt().trim());
    });
    return ResponseEntity.ok(Map.of("message", "Prompt updated"));
  }

  @PatchMapping("/password")
  public ResponseEntity<?> changePassword(
      @Valid
      @RequestBody
      ChangePasswordRequest request,

      Authentication authentication
  ) {
    String username = authentication != null ? authentication.getName() : null;
    if (username == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "Unauthorized"));
    }
    UserEntity user = authService.changePassword(username, request);
    emailService.sendVerificationToken(user);
    return ResponseEntity.ok(Map.of("message", "Check for email with token"));
  }

  private UserInfoResponse toResponse(UserEntity user) {
    List<String> roles = user.getRoles().stream()
        .map(RoleEntity::getName)
        .toList();
    List<UserPromptEntity> prompts = user.getPrompts();
    var latestCv = user.getCv();
    return new UserInfoResponse(
        user.getUsername(),
        user.getEmail(),
        user.getPhoneNumber(),
        user.isNotifyWhatsapp(),
        user.isNotifyEmail(),
        user.isEmailVerified(),
        user.getVerificationToken(),
        latestCv.getGptFileId(),
        latestCv.getGeminiFileId(),
        formatDateTime(user.getLastJobs()),
        user.getTimeInterval(),
        prompts.stream().map(p -> String.format("id: %d, engine: %s, prompt: %s", p.getId(), p.getEngine(), p.getPrompt())).toList(),
        formatDateTime(user.getCreatedAt()),
        roles
    );
  }

  private UserJobResponse toUserJobResponse(UserJobEntity userJob) {
    return new UserJobResponse(
        userJob.getJobUrl(),
        formatDateTime(userJob.getCreatedAt())
    );
  }

  private String formatDateTime(LocalDateTime dateTime) {
    return dateTime != null ? dateTime.toString() : null;
  }

  @PatchMapping("/approve")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> approveUser(
      @RequestParam("username")
      @NotBlank
      @Size(max = 255)
      String username,

      @RequestParam(value = "rejectReason", required = false)
      @Size(max = 255)
      String rejectReason
  ) {
    return userDataService.getUser(username)
        .map(user -> {
          if (Strings.isEmpty(rejectReason)) {
            if (user.isApproved()) {
              return ResponseEntity.badRequest().body(Map.of("error", "User already approved"));
            }
            user.setApproved(true);
            userDataService.updateUser(user);
            emailService.accountApproved(user);
          } else {
            emailService.accountRejected(user, rejectReason);
          }
          return ResponseEntity.ok(Map.of("message", "User approved"));
        })
        .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "User not found")));
  }

  @DeleteMapping("/delete")
  public ResponseEntity<?> deleteAccount(Authentication authentication) {
    String username = authentication != null ? authentication.getName() : null;
    if (username == null || username.isBlank()) {
      return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
    }
    userCvService.deleteUserCv(username);
    userDataService.deleteUserByUsername(username);
    return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
  }

}

