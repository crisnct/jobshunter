package com.jobshunter.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Handles OAuth2 authentication failures by redirecting to the frontend with an error message.
 */
@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException {

    log.warn("OAuth2 authentication failed: {}", exception.getMessage());

    String frontendUrl = determineFrontendUrl(request);

    // Extract a user-friendly error message
    String errorMessage = extractErrorMessage(exception);

    String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
        .queryParam("error", errorMessage)
        .build()
        .toUriString();

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }

  /**
   * Extract a user-friendly error message from the authentication exception.
   */
  private String extractErrorMessage(AuthenticationException exception) {
    String message = exception.getMessage();

    // Provide user-friendly messages for common OAuth2 errors
    if (message != null) {
      if (message.contains("access_denied")) {
        return "Access was denied. Please try again.";
      }
      if (message.contains("invalid_token")) {
        return "Invalid authentication token.";
      }
      if (message.contains("expired_token")) {
        return "Authentication session expired. Please try again.";
      }
      if (message.contains("Email is required")) {
        return "Email access is required for authentication.";
      }
    }

    return "Authentication failed. Please try again.";
  }

  /**
   * Determine the frontend URL to redirect to after OAuth authentication failure.
   */
  private String determineFrontendUrl(HttpServletRequest request) {
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
