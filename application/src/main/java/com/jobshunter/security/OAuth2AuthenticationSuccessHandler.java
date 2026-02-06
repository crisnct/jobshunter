package com.jobshunter.security;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserSessionEntity;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.database.service.UserSessionDBService;
import com.jobshunter.service.application.JwtService;
import com.jobshunter.service.application.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Handles successful OAuth2 authentication by generating JWT tokens and redirecting to the frontend.
 * The handler:
 * 1. Retrieves the authenticated user from the database
 * 2. Generates a new refresh token and stores the session
 * 3. Generates a JWT access token
 * 4. Sets cookies and redirects to the frontend
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final UserDBService userDBService;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final UserSessionDBService userSessionDBService;
  private final CookieService cookieService;
  private final ApplicationProperties properties;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException {

    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    Map<String, Object> attributes = oAuth2User.getAttributes();

    String email = (String) attributes.get("email");
    if (email == null) {
      log.error("OAuth2 authentication failed: email not found in OAuth2 user attributes");
      redirectToFrontendWithError(request, response, "Email not provided by OAuth2 provider");
      return;
    }

    // Find user by email (should exist as OAuth2UserService creates/links users)
    UserEntity user = userDBService.getUserByEmail(email).orElse(null);
    if (user == null) {
      log.error("OAuth2 authentication failed: user not found for email {}", email);
      redirectToFrontendWithError(request, response, "User not found");
      return;
    }

    // Check if user account is active
    if (user.isDeleted()) {
      log.warn("OAuth2 login attempt for deleted user: {}", user.getUsername());
      redirectToFrontendWithError(request, response, "Account has been deleted");
      return;
    }

    try {
      // Generate device ID and set cookie
      String deviceId = cookieService.generateNewDeviceId(response);

      // Get client information
      String userAgent = request.getHeader("User-Agent");
      String ipAddress = ClientIpResolver.resolveClientIp(request);

      // Generate refresh token
      String refreshToken = refreshTokenService.generateRefreshToken();
      String refreshTokenHash = refreshTokenService.hashRefreshToken(refreshToken);
      int expirationSec = properties.getSecurity().getRefreshToken().getExpirationSec();
      Instant expiresAt = Instant.now().plusSeconds(expirationSec);

      // Create or update session
      UserSessionEntity session = userSessionDBService.createOrUpdateSession(
          user, deviceId, refreshTokenHash, expiresAt, userAgent, ipAddress);

      // Set refresh token cookie
      cookieService.setRefreshTokenCookie(response, refreshToken);

      // Generate JWT access token with session ID
      String jwtToken = jwtService.generateToken(user, session.getId());

      log.info("OAuth2 login successful for user: {}", user.getUsername());

      // Redirect to frontend with success
      // The frontend will use the refresh endpoint to get the access token
      redirectToFrontendWithSuccess(request, response, jwtToken);

    } catch (Exception e) {
      log.error("Error during OAuth2 authentication success handling", e);
      redirectToFrontendWithError(request, response, "Authentication failed");
    }
  }

  private void redirectToFrontendWithSuccess(HttpServletRequest request, HttpServletResponse response,
      String accessToken) throws IOException {
    String frontendUrl = determineFrontendUrl(request);

    // Redirect with access token as a query parameter
    // The frontend will store this token and use it for API calls
    String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
        .queryParam("oauth_success", "true")
        .queryParam("access_token", accessToken)
        .build()
        .toUriString();

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }

  private void redirectToFrontendWithError(HttpServletRequest request, HttpServletResponse response,
      String errorMessage) throws IOException {
    String frontendUrl = determineFrontendUrl(request);

    String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
        .queryParam("error", errorMessage)
        .build()
        .toUriString();

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }

  /**
   * Determine the frontend URL to redirect to after OAuth authentication.
   * In production, this should be the configured frontend URL.
   * For local development, redirect to the root path.
   */
  private String determineFrontendUrl(HttpServletRequest request) {
    // Use the origin of the request or fallback to root
    String scheme = request.getScheme();
    String serverName = request.getServerName();
    int serverPort = request.getServerPort();

    // Check for forwarded headers (when behind a reverse proxy like ngrok)
    String forwardedProto = request.getHeader(JHHeaders.X_FORWARDED_PROTO);
    String forwardedHost = request.getHeader(JHHeaders.X_FORWARDED_HOST);

    if (forwardedProto != null && forwardedHost != null) {
      return forwardedProto + "://" + forwardedHost;
    }

    // Build URL from request
    StringBuilder url = new StringBuilder();
    url.append(scheme).append("://").append(serverName);

    // Only append port if non-standard
    if (!("http".equals(scheme) && serverPort == 80) &&
        !("https".equals(scheme) && serverPort == 443)) {
      url.append(":").append(serverPort);
    }

    return url.toString();
  }
}
