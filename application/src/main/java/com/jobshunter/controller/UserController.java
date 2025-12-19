package com.jobshunter.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.database.entities.RoleEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.service.AuthService;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.ChangePasswordRequest;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.dto.JobSearchRequest;
import com.jobshunter.dto.SearchJobsRequest;
import com.jobshunter.dto.SearchWithSerpRequest;
import com.jobshunter.dto.UserInfoResponse;
import com.jobshunter.dto.UserJobResponse;
import com.jobshunter.service.application.JobHuntService;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
  private com.jobshunter.service.application.UserCvService userCvService;

  @Autowired
  private JsonMapper mapper;

  @GetMapping("/me")
  public ResponseEntity<?> me(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
    }
    return userDataService.getUser(authentication.getName())
        .<ResponseEntity<?>>map(user -> ResponseEntity.ok(toResponse(user)))
        .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "User not found")));
  }

  @PostMapping("/search")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> search(@RequestBody SearchJobsRequest request) {
    if (Strings.isEmpty(request.username())) {
      return ResponseEntity.badRequest().body(Map.of("Error", "Missing username"));
    }
    return userDataService.getUser(request.username())
        .map(user -> jobHuntService.searchJobsForUser(user, request.iterations()))
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
  public ResponseEntity<?> getUserJobs(@RequestParam("username") String username) {
    if (username == null || username.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "username must not be blank"));
    }
    List<UserJobResponse> jobs = userDataService.getUserJobs(username).stream()
        .map(this::toUserJobResponse)
        .toList();
    return ResponseEntity.ok(jobs);
  }

  @PatchMapping("/time-interval")
  public ResponseEntity<?> setTimeInterval(@RequestParam("minutes") Integer minutes, Authentication authentication) {
    if (minutes == null || minutes <= 0) {
      return ResponseEntity.badRequest().body(Map.of("error", "minutes must be greater than zero"));
    }
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
  public ResponseEntity<?> setPrompt(@Valid @RequestBody JobSearchRequest request, Authentication authentication) {
    String username = authentication != null ? authentication.getName() : null;
    if (username == null) {
      return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
    }
    if (request == null || request.prompt().isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "prompt must not be blank"));
    }
    userDataService.getUser(username).ifPresent(user -> {
      user.setPrompt(request.prompt().trim());
      userDataService.updateUser(user);
    });
    return ResponseEntity.ok(Map.of("message", "Prompt updated"));
  }

  @PatchMapping("/serpApiRequest")
  public ResponseEntity<?> setSerpApiRequest(@RequestBody SearchWithSerpRequest request, Authentication authentication)
      throws JsonProcessingException {
    String username = authentication != null ? authentication.getName() : null;
    if (username == null) {
      return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
    }
    String valueString = mapper.writeValueAsString(request);

    userDataService.getUser(username).ifPresent(user -> {
      user.setSerpApiRequest(valueString);
      userDataService.updateUser(user);
    });
    return ResponseEntity.ok(Map.of("message", "Serp API request updated"));
  }

  @PatchMapping("/password")
  public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
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
    return new UserInfoResponse(
        user.getUsername(),
        user.getEmail(),
        user.getPhoneNumber(),
        user.isNotifyWhatsapp(),
        user.isNotifyEmail(),
        user.isEmailVerified(),
        user.getVerificationToken(),
        user.getCvFileId(),
        formatDateTime(user.getLastJobs()),
        user.getTimeInterval(),
        user.getPrompt(),
        user.getSerpApiRequest(),
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
      @RequestParam("username") String username,
      @RequestParam(value = "rejectReason", required = false) String rejectReason
  ) {
    if (username == null || username.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "username must not be blank"));
    }
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

