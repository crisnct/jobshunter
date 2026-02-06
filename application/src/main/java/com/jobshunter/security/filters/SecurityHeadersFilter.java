package com.jobshunter.security.filters;

import com.jobshunter.security.JHHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Security headers filter to protect against common web vulnerabilities. Implements comprehensive security headers to prevent:
 * {@snippet :
 * - Clickjacking attacks (X-Frame-Options)
 * - XSS attacks (X-XSS-Protection, Content-Security-Policy)
 * - MIME type sniffing (X-Content-Type-Options)
 * - Information disclosure (X-Permitted-Cross-Domain-Policies)
 * - Referrer information leakage (Referrer-Policy)
 *}
 * Note: HSTS (Strict-Transport-Security) is configured via Spring Security in SecurityConfig.
 *
 * @author Security Team
 */
public class SecurityHeadersFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // Add comprehensive security headers
    addSecurityHeaders(request, response);

    filterChain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    // No cleanup needed
  }

  /**
   * Add comprehensive security headers to the response.
   *
   * @param request  the HTTP request
   * @param response the HTTP response
   */
  private void addSecurityHeaders(HttpServletRequest request, HttpServletResponse response) {

    // 1. X-Frame-Options: Prevent clickjacking attacks
    response.setHeader(JHHeaders.X_FRAME_OPTIONS, "DENY");

    // 2. X-Content-Type-Options: Prevent MIME type sniffing
    response.setHeader(JHHeaders.X_CONTENT_TYPE_OPTIONS, "nosniff");

    // 3. X-XSS-Protection: Enable XSS filtering (legacy browsers)
    response.setHeader(JHHeaders.X_XSS_PROTECTION, "1; mode=block");

    // 4. Content-Security-Policy: Comprehensive XSS protection
    String csp = buildContentSecurityPolicy(request);
    response.setHeader(JHHeaders.CONTENT_SECURITY_POLICY, csp);

    // 5. Referrer-Policy: Control referrer information
    response.setHeader(JHHeaders.REFERRER_POLICY, "strict-origin-when-cross-origin");

    // 6. X-Permitted-Cross-Domain-Policies: Restrict cross-domain policies
    response.setHeader(JHHeaders.X_PERMITTED_CROSS_DOMAIN_POLICIES, "none");

    // 7. Permissions-Policy: Control browser features
    response.setHeader(JHHeaders.PERMISSIONS_POLICY,
        "geolocation=(), microphone=(), camera=(), payment=(), usb=(), magnetometer=(), gyroscope=(), accelerometer=()");

    // 8. Cross-Origin-Embedder-Policy: Prevent cross-origin embedding
    response.setHeader(JHHeaders.CROSS_ORIGIN_EMBEDDER_POLICY, "require-corp");

    // 9. Cross-Origin-Opener-Policy: Isolate browsing context
    response.setHeader(JHHeaders.CROSS_ORIGIN_OPENER_POLICY, "same-origin");

    // 10. Cross-Origin-Resource-Policy: Control cross-origin resource access
    response.setHeader(JHHeaders.CROSS_ORIGIN_RESOURCE_POLICY, "same-origin");
  }

  /**
   * Build a comprehensive Content Security Policy.
   *
   * @param request the HTTP request
   * @return the CSP header value
   */
  private String buildContentSecurityPolicy(HttpServletRequest request) {

    // Default source restrictions

    String csp = "default-src 'self'"

        // Script sources - allow self and inline scripts for React
        + "; script-src 'self' 'unsafe-inline' 'unsafe-eval'"

        // Style sources - allow self and inline styles for React
        + "; style-src 'self' 'unsafe-inline'"

        // Image sources - allow self and data URIs
        + "; img-src 'self' data: https:"

        // Font sources - allow self
        + "; font-src 'self'"

        // Connect sources - allow self and API endpoints
        + "; connect-src 'self'"

        // Object sources - deny all
        + "; object-src 'none'"

        // Base URI - restrict to self
        + "; base-uri 'self'"

        // Form action - restrict to self
        + "; form-action 'self'"

        // Frame ancestors - deny all (redundant with X-Frame-Options but more specific)
        + "; frame-ancestors 'none'"

        // Upgrade insecure requests
        + "; upgrade-insecure-requests";

    return csp;
  }

}
