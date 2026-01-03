package com.jobshunter.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeviceCookieService {

  public static final String DEVICE_ID_COOKIE = "device_id";

  private final int maxAge;

  public DeviceCookieService(
      @Value("${security.jwt.expiration-ms:}") String jwtExpirationMs
  ) {
    this.maxAge = (int)(Long.parseLong(jwtExpirationMs) / 1000);
  }

  public String generateNewDeviceId(HttpServletResponse response) {
    String deviceId = UUID.randomUUID().toString();

    Cookie cookie = new Cookie(DEVICE_ID_COOKIE, deviceId);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge(maxAge);

    // SameSite not available directly on Cookie pre-Servlet 6
    response.addHeader("Set-Cookie",
        DEVICE_ID_COOKIE + "=" + deviceId +
            "; Max-Age=" + maxAge +
            "; Path=/" +
            "; Secure" +
            "; HttpOnly" +
            "; SameSite=Lax");
    response.addCookie(cookie); // fallback for older containers
    return deviceId;
  }

  public void expireDeviceCookie(HttpServletResponse response) {
    Cookie cookie = new Cookie(DEVICE_ID_COOKIE, "");
    cookie.setPath("/");
    cookie.setMaxAge(0);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    response.addCookie(cookie);
  }
}
