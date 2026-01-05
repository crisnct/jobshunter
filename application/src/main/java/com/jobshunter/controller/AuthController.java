package com.jobshunter.controller;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.AuthDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.dto.AuthResponse;
import com.jobshunter.dto.LoginRequest;
import com.jobshunter.dto.RegisterRequest;
import com.jobshunter.dto.RegistrationResponse;
import com.jobshunter.processor.SqlInjectionSafe;
import com.jobshunter.security.DeviceCookieService;
import com.jobshunter.security.JHHeaders;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthDBService authDBService;

  private final EmailNotifierService emailService;

  private final CookieCsrfTokenRepository csrfTokenRepository;

  private final UserDBService userDBService;

  private final DeviceCookieService deviceCookieService;

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
    String token = authDBService.login(request);
    String deviceId = deviceCookieService.generateNewDeviceId(httpResponse);
    String ip = httpResponse.getHeader(JHHeaders.IP_HEADER);
    String userAgent = httpRequest.getHeader(JHHeaders.USER_AGENT);
    userDBService.updateDeviceId(request.username(), deviceId, ip, userAgent);
    log.info("Login for {} from IP {}", request.username(), httpResponse.getHeader(JHHeaders.IP_HEADER));
    return new AuthResponse(token);
  }

  @PostMapping("/logout")
  public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
    SecurityContextHolder.clearContext();
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    expireCookie(response, "JSESSIONID");
    deviceCookieService.expireDeviceCookie(response);
    response.setStatus(HttpServletResponse.SC_OK);
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

    log.info("Generated new csrf token for {}", response.getHeader(JHHeaders.IP_HEADER));
    return new ResponseEntity<>(tokenMap, HttpStatus.OK);
  }

  private void expireCookie(HttpServletResponse response, String name) {
    Cookie c = new Cookie(name, "");
    c.setPath("/");
    c.setMaxAge(0);
    c.setHttpOnly(true);
    c.setSecure(true);
    response.addCookie(c);
  }

}
