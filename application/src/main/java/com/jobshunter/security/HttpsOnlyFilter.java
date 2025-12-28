package com.jobshunter.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpsOnlyFilter extends OncePerRequestFilter {

  @Value("${security.httpsOnly.enabled:true}")
  private boolean httpsOnlyEnabled;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    // Skip HTTPS enforcement if disabled (e.g., in test environment via application-test.yml)
    if (!httpsOnlyEnabled || request.isSecure()) {
      filterChain.doFilter(request, response);
    } else {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS is required");
    }
  }

}
