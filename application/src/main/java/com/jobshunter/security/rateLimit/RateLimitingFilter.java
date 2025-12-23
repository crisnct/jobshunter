package com.jobshunter.security.rateLimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

  private static final Duration COOLDOWN = Duration.ofHours(8);

  private final InMemoryRateLimiter rateLimiter;
  private final CooldownRegistry cooldownRegistry;

  public RateLimitingFilter(
      InMemoryRateLimiter rateLimiter,
      CooldownRegistry cooldownRegistry) {
    this.rateLimiter = rateLimiter;
    this.cooldownRegistry = cooldownRegistry;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String clientKey = resolveClientKey(request);

    // 1️⃣ Cooldown check
    if (cooldownRegistry.isBlocked(clientKey)) {
      reject(response, cooldownRegistry.secondsLeft(clientKey));
      return;
    }

    // 2️⃣ Rate limit check
    Bucket bucket = rateLimiter.resolveBucket(clientKey);
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    if (probe.isConsumed()) {
      response.setHeader(
          "X-Rate-Limit-Remaining",
          String.valueOf(probe.getRemainingTokens())
      );
      filterChain.doFilter(request, response);
      return;
    }

    // 3️⃣ Activate cooldown
    cooldownRegistry.block(clientKey, COOLDOWN);
    reject(response, COOLDOWN.getSeconds());
  }

  private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
    response.setStatus(429);
    response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
    response.setContentType("application/json");
    response.getWriter().write(
        "{\"message\":\"Too many requests. Try again later.\"}"
    );
  }

  private String resolveClientKey(HttpServletRequest request) {
    String apiKey = request.getHeader("X-API-Key");
    if (apiKey != null && !apiKey.isBlank()) {
      return "apiKey:" + apiKey.trim();
    }
    return "ip:" + request.getRemoteAddr();
  }
}
