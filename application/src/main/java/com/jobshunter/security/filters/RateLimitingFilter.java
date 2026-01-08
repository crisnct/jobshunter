package com.jobshunter.security.filters;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.security.ClientIpResolver;
import com.jobshunter.security.JHHeaders;
import com.jobshunter.security.rateLimitBucket4J.BlockRegistry;
import com.jobshunter.security.rateLimitBucket4J.InMemoryRateLimiter;
import com.jobshunter.security.rateLimitBucket4J.ViolationRegistry;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

  private static final Duration BLOCK_4_HOURS = Duration.ofHours(4);

  private static final Duration BLOCK_48_HOURS = Duration.ofHours(48);

  //backforce - put clients to wait too much if they exceed rate limit
  private static final Duration MAX_WAIT = Duration.ofMinutes(2);

  private final InMemoryRateLimiter rateLimiter;
  private final ViolationRegistry violationRegistry;
  private final BlockRegistry blockRegistry;
  private final ApplicationProperties properties;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    String clientKey = ClientIpResolver.resolveClientIp(request);

    // 🔴 1. Hard block
    if (blockRegistry.isBlocked(clientKey)) {
      reject(response, blockRegistry.secondsLeft(clientKey));
      return;
    }

    Bucket bucket = rateLimiter.resolveBucket(clientKey);

    boolean allowed;
    try {
      // 🟢 2. WAIT instead of reject
      allowed = bucket.asBlocking().tryConsume(1, MAX_WAIT);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      reject(response, 1);
      return;
    }

    if (allowed) {
      filterChain.doFilter(request, response);
      return;
    }

    // 🔥 3. Only now it's a violation (client waited but still abusive)
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

    // Prima violare → 429 simplu
    reject(response, properties.getJobsHunter().getRateLimit().getCapacity());
  }

  private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader(JHHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
    response.setContentType("application/json");
    response.getWriter().write(
        "{\"message\":\"Rate limit exceeded. Please slow down.\"}"
    );
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/css/")
        || path.startsWith("/js/")
        || path.startsWith("/images/")
        || path.startsWith("/swagger")
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/api/auth")
        || "OPTIONS".equalsIgnoreCase(request.getMethod());
  }
}
