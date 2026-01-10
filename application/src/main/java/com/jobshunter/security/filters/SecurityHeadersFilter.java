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
 * - Clickjacking attacks(X-Frame-Options)
 * - XSS attacks (X-XSS-Protection, Content-Security-Policy)
 * - MIME type sniffing (X-Content-Type-Options)
 * - Protocol downgrade attacks (Strict-Transport-Security)
 * - Information disclosure (X-Permitted-Cross-Domain-Policies)
 * - Referrer information leakage (Referrer-Policy)
 *}
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

    // 4. Strict-Transport-Security: Force HTTPS (only for HTTPS requests)
    if (request.isSecure() || "https".equalsIgnoreCase(request.getHeader(JHHeaders.X_FORWARDED_PROTO))) {
      response.setHeader(JHHeaders.STRICT_TRANSPORT_SECURITY, "max-age=31536000; includeSubDomains; preload");
    }

    // 5. Content-Security-Policy: Comprehensive XSS protection
    String csp = buildContentSecurityPolicy(request);
    response.setHeader(JHHeaders.CONTENT_SECURITY_POLICY, csp);

    // 6. Referrer-Policy: Control referrer information
    response.setHeader(JHHeaders.REFERRER_POLICY, "strict-origin-when-cross-origin");

    // 7. X-Permitted-Cross-Domain-Policies: Restrict cross-domain policies
    response.setHeader(JHHeaders.X_PERMITTED_CROSS_DOMAIN_POLICIES, "none");

    // 8. Permissions-Policy: Control browser features
    response.setHeader(JHHeaders.PERMISSIONS_POLICY,
        "geolocation=(), microphone=(), camera=(), payment=(), usb=(), magnetometer=(), gyroscope=(), accelerometer=()");

    // 9. Cross-Origin-Embedder-Policy: Prevent cross-origin embedding
    response.setHeader(JHHeaders.CROSS_ORIGIN_EMBEDDER_POLICY, "require-corp");

    // 10. Cross-Origin-Opener-Policy: Isolate browsing context
    response.setHeader(JHHeaders.CROSS_ORIGIN_OPENER_POLICY, "same-origin");

    // 11. Cross-Origin-Resource-Policy: Control cross-origin resource access
    response.setHeader(JHHeaders.CROSS_ORIGIN_RESOURCE_POLICY, "same-origin");
  }

  /**
   * Build a comprehensive Content Security Policy.
   *
   * @param request the HTTP request
   * @return the CSP header value
   */
  private String buildContentSecurityPolicy(HttpServletRequest request) {
    StringBuilder csp = new StringBuilder();

    // Default source restrictions
    csp.append("default-src 'self'");

    // Script sources - allow self and inline scripts for React
    csp.append("; script-src 'self' 'unsafe-inline' 'unsafe-eval'");

    // Style sources - allow self and inline styles for React
    csp.append("; style-src 'self' 'unsafe-inline'");

    // Image sources - allow self and data URIs
    csp.append("; img-src 'self' data: https:");

    // Font sources - allow self
    csp.append("; font-src 'self'");

    // Connect sources - allow self and API endpoints
    csp.append("; connect-src 'self'");

    // Object sources - deny all
    csp.append("; object-src 'none'");

    // Base URI - restrict to self
    csp.append("; base-uri 'self'");

    // Form action - restrict to self
    csp.append("; form-action 'self'");

    // Frame ancestors - deny all (redundant with X-Frame-Options but more specific)
    csp.append("; frame-ancestors 'none'");

    // Upgrade insecure requests
    csp.append("; upgrade-insecure-requests");

    return csp.toString();
  }

}
