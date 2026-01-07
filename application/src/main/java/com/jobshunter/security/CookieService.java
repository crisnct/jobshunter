package com.jobshunter.security;

import com.jobshunter.ApplicationProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
public class CookieService {

  public static final String SESSION_ID_COOKIE = "JSESSIONID";
  public static final String DEVICE_ID_COOKIE = "device_id";
  public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

  private final ApplicationProperties properties;

  public CookieService(
      ApplicationProperties properties
  ) {
    this.properties = properties;
  }

  // =========================
  // Device ID
  // =========================

  public String generateNewDeviceId(HttpServletResponse response) {
    String deviceId = UUID.randomUUID().toString();

    ResponseCookie cookie = baseCookie(DEVICE_ID_COOKIE, deviceId)
        .path("/")
        .maxAge(properties.getSecurity().getCookie().getDeviceId().getExpirationSec())
        .build();

    response.addHeader(JHHeaders.SET_COOKIE, cookie.toString());
    return deviceId;
  }

  // =========================
  // Refresh Token
  // =========================

  public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
    ResponseCookie cookie = baseCookie(REFRESH_TOKEN_COOKIE, refreshToken)
        .path("/api/auth")
        .maxAge(properties.getSecurity().getRefreshToken().getExpirationSec())
        .build();

    response.addHeader(JHHeaders.SET_COOKIE, cookie.toString());
  }

  public Optional<String> getCookie(HttpServletRequest httpRequest, String refreshTokenCookie) {
    return Optional.ofNullable(WebUtils.getCookie(httpRequest, refreshTokenCookie)).map(Cookie::getValue);
  }

  public void expireCookies(HttpServletResponse response) {
    expireCookie(response, SESSION_ID_COOKIE, "/");
    expireCookie(response, DEVICE_ID_COOKIE, "/");
    expireCookie(response, REFRESH_TOKEN_COOKIE, "/api/auth");
  }

  // =========================
  // Helpers
  // =========================

  private ResponseCookie.ResponseCookieBuilder baseCookie(String name, String value) {
    return ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(true)
        .sameSite("Lax");
  }

  private void expireCookie(HttpServletResponse response, String name, String path) {
    ResponseCookie cookie = baseCookie(name, "")
        .path(path)
        .maxAge(Duration.ZERO)
        .build();

    response.addHeader(JHHeaders.SET_COOKIE, cookie.toString());
  }

}
