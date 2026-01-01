package com.jobshunter.security.rateLimitBucket4J;

import com.jobshunter.ApplicationProperties;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

  private static final Duration BLOCK_4_HOURS = Duration.ofHours(4);

  private static final Duration BLOCK_48_HOURS = Duration.ofHours(48);

  private final InMemoryRateLimiter rateLimiter;
  private final ViolationRegistry violationRegistry;
  private final BlockRegistry blockRegistry;
  private final ApplicationProperties properties;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String clientKey = response.getHeader("JHIPHEADER");

    // 🔴 1. Check hard blocks
    if (blockRegistry.isBlocked(clientKey)) {
      reject(response, blockRegistry.secondsLeft(clientKey));
      return;
    }

    // 🟢 2. Normal rate limiting
    Bucket bucket = rateLimiter.resolveBucket(clientKey);
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    if (probe.isConsumed()) {
      filterChain.doFilter(request, response);
      return;
    }

    // 🔥 3. Violation detected
    int violations = violationRegistry.increment(clientKey);

    if (violations == 2) {
      blockRegistry.block(clientKey, BLOCK_4_HOURS);
      reject(response, BLOCK_4_HOURS.getSeconds());
      return;
    }

    if (violations >= 3) {
      blockRegistry.block(clientKey, BLOCK_48_HOURS);
      reject(response, BLOCK_48_HOURS.getSeconds());
      return;
    }

    // Prima încălcare → doar rate limit normal
    reject(response, properties.getJobsHunter().getRateLimit().getCapacity());
  }

  private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
    response.setContentType("application/json");
    response.getWriter().write(
        "{\"message\":\"Rate LLimit Exceeded. Access temporarily restricted.\"}"
    );
  }

}
