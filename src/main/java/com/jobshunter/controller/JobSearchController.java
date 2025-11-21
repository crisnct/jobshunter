package com.jobshunter.controller;

import com.jobshunter.database.repository.UserRepository;
import com.jobshunter.dto.JobSearchRequest;
import com.jobshunter.service.JobHuntOrchestrator;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class JobSearchController {

  private final JobHuntOrchestrator orchestrator;

  private final UserRepository userRepository;

  @PostMapping("/search")
  public ResponseEntity<Void> search(
      @RequestParam(name = "notifyOnWhatsupp", required = false, defaultValue = "false") Boolean notifyOnWhatsupp
  ) throws InterruptedException {
    orchestrator.searchJobsForAll(false, notifyOnWhatsupp);
    return ResponseEntity.ok(null);
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
    userRepository.findByUsername(username).ifPresent(user -> {
      user.setTimeInterval(minutes);
      userRepository.save(user);
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
    userRepository.findByUsername(username).ifPresent(user -> {
      user.setPrompt(request.prompt().trim());
      userRepository.save(user);
    });
    return ResponseEntity.ok(Map.of("message", "Prompt updated"));
  }

  @GetMapping("/status")
  public ResponseEntity<?> status() {
    return Optional.ofNullable(orchestrator.lastRun())
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.ok(Map.of("message", "No search executed yet")));
  }
}
