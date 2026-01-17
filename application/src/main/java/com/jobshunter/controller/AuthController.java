package com.jobshunter.controller;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.AuthDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.database.service.UserSessionDBService;
import com.jobshunter.dto.AuthResponse;
import com.jobshunter.dto.LoginRequest;
import com.jobshunter.dto.LoginResult;
import com.jobshunter.dto.RegisterRequest;
import com.jobshunter.dto.RegistrationResponse;
import com.jobshunter.processor.SqlInjectionSafe;
import com.jobshunter.security.CookieService;
import com.jobshunter.security.JHHeaders;
import com.jobshunter.service.application.JwtService;
import com.jobshunter.service.application.RefreshTokenService;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthDBService authDBService;

  private final EmailNotifierService emailService;

  private final CookieCsrfTokenRepository csrfTokenRepository;

  private final UserDBService userDBService;

  private final CookieService cookieService;

  private final RefreshTokenService refreshTokenService;

  private final UserSessionDBService userSessionDBService;

  private final JwtService jwtService;

  @PostMapping("/register")
  public RegistrationResponse register(
      @Valid
      @RequestBody
      RegisterRequest request
  ) {
    UserEntity user = authDBService.register(request);
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
    // Get or generate device ID
    String deviceId = getOrGenerateDeviceId(httpRequest, httpResponse);
    String ip = httpResponse.getHeader(JHHeaders.X_REAL_IP);
    String userAgent = httpRequest.getHeader(JHHeaders.USER_AGENT);

    // Perform login and create session
    LoginResult loginResult = authDBService.login(request, deviceId, userAgent, ip);

    // Set refresh token cookie
    cookieService.setRefreshTokenCookie(httpResponse, loginResult.refreshToken());

    log.info("Login for {} from IP {}", request.username(), ip);
    return new AuthResponse(loginResult.accessToken());
  }

  @PostMapping("/refresh")
  public AuthResponse refresh(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse
  ) {
    // Get refresh token from cookie
    Optional<String> refreshTokenOp = cookieService.getCookie(httpRequest, CookieService.REFRESH_TOKEN_COOKIE);
    if (refreshTokenOp.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found");
    }

    // Get device ID from cookie
    String deviceId = getDeviceIdFromCookie(httpRequest);
    if (deviceId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Device ID not found");
    }

    // Get user from valid refresh token (endpoint is public, no security context)
    UserEntity user = refreshTokenService.validateAndGetUser(deviceId, refreshTokenOp.get());

    // Validate and rotate refresh token
    String newRefreshToken = refreshTokenService.validateAndRotate(user, deviceId, refreshTokenOp.get());

    // Generate new access token
    String jwtToken = jwtService.generateToken(user);

    // Set new refresh token cookie
    cookieService.setRefreshTokenCookie(httpResponse, newRefreshToken);

    log.debug("Token refreshed for user: {}", user.getUsername());
    return new AuthResponse(jwtToken);
  }

  @PostMapping("/logout")
  public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get current user
    String username = SecurityContextHolder.getContext().getAuthentication() != null
        ? SecurityContextHolder.getContext().getAuthentication().getName()
        : null;

    if (username != null) {
      // Get device ID
      String deviceId = getDeviceIdFromCookie(request);
      if (deviceId != null) {
        userDBService.getUser(username)
            .ifPresent(user -> userSessionDBService.revokeSession(user, deviceId, "LOGOUT"));
      }
    }

    SecurityContextHolder.clearContext();
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    cookieService.expireCookies(response);
    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }

  @PostMapping("/logout-all")
  @PreAuthorize("hasRole('ADMIN')")
  public void logoutAll(HttpServletRequest request, HttpServletResponse response) {
    // Get current user
    String username = SecurityContextHolder.getContext().getAuthentication() != null
        ? SecurityContextHolder.getContext().getAuthentication().getName()
        : null;

    if (username != null) {
      userDBService.getUser(username)
          .ifPresent(user -> userSessionDBService.revokeAllUserSessions(user, "LOGOUT_ALL"));
    }

    SecurityContextHolder.clearContext();
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    cookieService.expireCookies(response);
    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }

  @PatchMapping("/verify")
  public ResponseEntity<Map<String, String>> verify(
      @RequestParam("token")
      @NotBlank
      @SqlInjectionSafe
      @Size(max = 128)
      String token
  ) {
    authDBService.verifyEmail(token);
    return ResponseEntity.ok(Map.of("message", "Email verified. Please wait up to 72h until your account is approved by an admin."));
  }

  /**
   * Get CSRF token for frontend requests. Provides the CSRF token needed for secure state-changing API calls.
   *
   * @return ResponseEntity containing the CSRF token metadata
   */
  @GetMapping("/csrf-token")
  public ResponseEntity<Map<String, String>> getCsrfToken(HttpServletRequest request, HttpServletResponse response) {
    CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
    csrfTokenRepository.saveToken(csrfToken, request, response);

    Map<String, String> tokenMap = new HashMap<>();
    tokenMap.put("token", csrfToken.getToken());
    tokenMap.put("headerName", csrfToken.getHeaderName());
    tokenMap.put("parameterName", csrfToken.getParameterName());

    log.info("Generated new csrf token for {}", response.getHeader(JHHeaders.X_REAL_IP));
    return new ResponseEntity<>(tokenMap, HttpStatus.OK);
  }

  private String getOrGenerateDeviceId(HttpServletRequest request, HttpServletResponse response) {
    String deviceId = getDeviceIdFromCookie(request);
    if (deviceId == null) {
      deviceId = cookieService.generateNewDeviceId(response);
    }
    return deviceId;
  }

  private String getDeviceIdFromCookie(HttpServletRequest request) {
    return cookieService.getCookie(request, CookieService.DEVICE_ID_COOKIE).orElse(null);
  }

}
