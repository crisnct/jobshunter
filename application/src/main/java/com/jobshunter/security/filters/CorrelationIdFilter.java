package com.jobshunter.security.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that generates a correlation ID for each HTTP request.
 * The correlation ID is stored in MDC for logging and returned in response header.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

  public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  public static final String CORRELATION_ID_MDC_KEY = "correlationId";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    // Check if correlation ID already exists in request header (from upstream service)
    String correlationId = request.getHeader(CORRELATION_ID_HEADER);
    if (Strings.isBlank(correlationId)) {
      correlationId = UUID.randomUUID().toString();
    }

    // Add to MDC for logging
    MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

    // Add to response header for client reference
    response.setHeader(CORRELATION_ID_HEADER, correlationId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(CORRELATION_ID_MDC_KEY);
    }
  }
}
