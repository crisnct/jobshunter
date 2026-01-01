package com.jobshunter.controller;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.AuthService;
import com.jobshunter.dto.AuthResponse;
import com.jobshunter.dto.LoginRequest;
import com.jobshunter.dto.RegisterRequest;
import com.jobshunter.dto.RegistrationResponse;
import com.jobshunter.processor.SqlInjectionSafe;
import com.jobshunter.security.SecurityHeadersFilter;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
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

  private static final String DEVICE_ID_COOKIE = "device_id";

  private final AuthService authService;

  private final EmailNotifierService emailService;

  private final CookieCsrfTokenRepository csrfTokenRepository;

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
      LoginRequest request,

      HttpServletRequest httpRequest,

      HttpServletResponse httpResponse
  ) {
    String token = authService.login(request);
    log.info("Login for {} from IP {}", request.username(), httpResponse.getHeader(SecurityHeadersFilter.IP_HEADER));
    return new AuthResponse(token);
  }

  @PatchMapping("/verify")
  public ResponseEntity<Map<String, String>> verify(
      @RequestParam("token")
      @NotBlank
      @SqlInjectionSafe
      @Size(max = 128)
      String token
  ) {
    authService.verifyEmail(token);
    return ResponseEntity.ok(Map.of("message", "Email verified. Please wait up to 72h until your account is approved by an admin."));
  }

  /**
   * Get CSRF token for frontend requests. Provides the CSRF token needed for secure state-changing API calls.
   *
   * @return ResponseEntity containing the CSRF token metadata
   */
  @GetMapping("/csrf-token")
  @Operation(
      summary = "Get CSRF token",
      description = "Retrieves the CSRF token required for secure API requests"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "CSRF token retrieved successfully",
          content = @Content(schema = @Schema(implementation = Map.class)))
  })
  public ResponseEntity<Map<String, String>> getCsrfToken(HttpServletRequest request, HttpServletResponse response) {
    CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
    csrfTokenRepository.saveToken(csrfToken, request, response);

    Map<String, String> tokenMap = new HashMap<>();
    tokenMap.put("token", csrfToken.getToken());
    tokenMap.put("headerName", csrfToken.getHeaderName());
    tokenMap.put("parameterName", csrfToken.getParameterName());

    log.info("Generated new csrf token for {}", response.getHeader(SecurityHeadersFilter.IP_HEADER));
    return new ResponseEntity<>(tokenMap, HttpStatus.OK);
  }

  private Optional<String> getDeviceId(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return Optional.empty();
    }
    return Arrays.stream(request.getCookies())
        .filter(c -> DEVICE_ID_COOKIE.equals(c.getName()))
        .map(Cookie::getValue)
        .findFirst();
  }

  private String ensureDeviceId(
      HttpServletRequest request,
      HttpServletResponse response
  ) {

    return getDeviceId(request).orElseGet(() -> {
      String deviceId = UUID.randomUUID().toString();

      Cookie cookie = new Cookie(DEVICE_ID_COOKIE, deviceId);
      cookie.setHttpOnly(true);
      cookie.setSecure(true);
      cookie.setPath("/");
      cookie.setMaxAge(365 * 24 * 60 * 60); // 1 year

      // SameSite not available directly on Cookie pre-Servlet 6
      response.addHeader("Set-Cookie",
          DEVICE_ID_COOKIE + "=" + deviceId +
              "; Max-Age=31536000" +
              "; Path=/" +
              "; Secure" +
              "; HttpOnly" +
              "; SameSite=Lax");

      response.addCookie(cookie); // fallback for older containers

      return deviceId;
    });
  }


}
