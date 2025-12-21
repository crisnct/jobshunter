package com.jobshunter.controller;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.AuthService;
import com.jobshunter.dto.AuthResponse;
import com.jobshunter.dto.LoginRequest;
import com.jobshunter.dto.RegisterRequest;
import com.jobshunter.dto.RegistrationResponse;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  @Autowired
  private AuthService authService;

  @Autowired
  private EmailNotifierService emailService;

  @PostMapping("/register")
  public RegistrationResponse register(
      @Valid
      @RequestBody
      RegisterRequest request
  ) {
    UserEntity user = authService.register(request);
    emailService.sendVerificationToken(user);
    log.info("Verification token for {} is {}", user.getEmail(), user.getVerificationToken());
    return new RegistrationResponse(
        "User registered. Please verify your email using the token sent via email (check logs in dev).",
        user.getVerificationToken());
  }

  @PostMapping("/login")
  public AuthResponse login(
      @Valid
      @RequestBody
      LoginRequest request
  ) {
    String token = authService.login(request);
    return new AuthResponse(token);
  }

  @PatchMapping("/verify")
  public ResponseEntity<Map<String, String>> verify(
      @RequestParam("token")
      @NotBlank
      @Size(max = 128)
      String token
  ) {
    authService.verifyEmail(token);
    return ResponseEntity.ok(Map.of("message", "Email verified. Please wait up to 72h until your account is approved by an admin."));
  }

}
