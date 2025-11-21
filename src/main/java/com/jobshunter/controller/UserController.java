package com.jobshunter.controller;

import com.jobshunter.database.entities.Role;
import com.jobshunter.database.entities.User;
import com.jobshunter.database.repository.UserRepository;
import com.jobshunter.dto.UserInfoResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

  private final UserRepository userRepository;

  @GetMapping("/me")
  public ResponseEntity<?> me(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
    }
    return userRepository.findByUsername(authentication.getName())
        .<ResponseEntity<?>>map(user -> ResponseEntity.ok(toResponse(user)))
        .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "User not found")));
  }

  private UserInfoResponse toResponse(User user) {
    List<String> roles = user.getRoles().stream()
        .map(Role::getName)
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
