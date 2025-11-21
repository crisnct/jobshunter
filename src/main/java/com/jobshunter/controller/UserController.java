package com.jobshunter.controller;

import com.jobshunter.database.entities.RoleEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.dto.JobSearchRequest;
import com.jobshunter.dto.UserInfoResponse;
import com.jobshunter.service.application.JobHuntService;
import com.jobshunter.service.clients.WhatsAppNotifier;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
public class UserController {

  @Autowired
  private UserDataService userDataService;

  @Autowired
  private JobHuntService jobHuntService;

  @Autowired
  private WhatsAppNotifier whatsAppNotifier;

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
  public ResponseEntity<JobHuntResponse> search(
      @RequestParam(name = "notifyOnWhatsupp", required = false, defaultValue = "false") Boolean notifyOnWhatsupp,
      Authentication authentication
  ) {
    String username = authentication != null ? authentication.getName() : null;
    if (username == null) {
      return ResponseEntity.badRequest().build();
    } else {
      Optional<UserEntity> userOp = userDataService.getUser(username);
      if (userOp.isPresent()){
        //noinspection OptionalGetWithoutIsPresent
        JobHuntResponse jobs = jobHuntService.searchJobsForUser(false, userOp.get(), 1).get();
        if (notifyOnWhatsupp && !jobs.jobsFound().isEmpty()) {
          whatsAppNotifier.send(jobs.jobsFound(), username);
        }
        return ResponseEntity.ok(jobs);
      } else {
        return ResponseEntity.badRequest().build();
      }
    }
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

  @PostMapping("/prompt")
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

  private UserInfoResponse toResponse(UserEntity user) {
    List<String> roles = user.getRoles().stream()
        .map(RoleEntity::getName)
        .toList();
    return new UserInfoResponse(
        user.getUsername(),
        user.getEmail(),
        user.getPhoneNumber(),
        user.isEmailVerified(),
        user.getVerificationToken(),
        user.getCvFileId(),
        user.getLastJobs(),
        user.getTimeInterval(),
        user.getPrompt(),
        user.getCreatedAt(),
        roles
    );
  }
}
