package com.jobshunter.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public final class UrlAffinityExecutor {

  private static final int MAX_CONCURRENT_TASKS = 50;

  //minutes
  public static final int LIFETIME_IN_CACHE = 3;

  public static final int REFRESH_PERIOD_SAME_HOST = 5000;

  private final Semaphore globalSemaphore = new Semaphore(MAX_CONCURRENT_TASKS, true);

  private final Cache<String, HostExecutionContext> cache;

  public UrlAffinityExecutor() {
    this.cache =
        Caffeine.newBuilder()
            .expireAfterAccess(LIFETIME_IN_CACHE, TimeUnit.MINUTES)
            .removalListener((String _, HostExecutionContext ctx, RemovalCause _) -> {
              ctx.executor.shutdown(); // graceful shutdown
            })
            .build();
  }

  public <T> CompletableFuture<T> submit(String url, Supplier<T> task) {
    try {
      globalSemaphore.acquire(); // Wait if 20 threads are busy
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    String host = this.extractHost(url);
    HostExecutionContext context = cache.get(host, this::createContext);
    CompletableFuture<T> future = new CompletableFuture<>();
    Supplier<T> rateLimitedTask = RateLimiter.decorateSupplier(context.rateLimiter, task);
    context.executor.submit(() -> {
      try {
        T result = rateLimitedTask.get();
        future.complete(result);
      } catch (Throwable ex) {
        future.completeExceptionally(ex);
      } finally {
        globalSemaphore.release(); // Free the slot
      }
    });
    return future;
  }

  // =========================
  // Context creation
  // =========================
  private HostExecutionContext createContext(String host) {
    ExecutorService executor =
        Executors.newSingleThreadExecutor(
            Thread.ofVirtual().name("host-" + host, 0).factory()
        );

    RateLimiter rateLimiter =
        RateLimiter.of(
            "rl-" + host,
            RateLimiterConfig.custom()
                // 1 request at 5 seconds per host
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofSeconds(REFRESH_PERIOD_SAME_HOST))
                .timeoutDuration(Duration.ofMinutes(LIFETIME_IN_CACHE))
                .build()
        );

    return new HostExecutionContext(executor, rateLimiter);
  }

  private String extractHost(String url) {
    return URI.create(url).getHost();
  }

  public List<ExecutorService> getAllExecutors() {
    return cache.asMap().values().stream().map(p -> p.executor).toList();
  }

  // =========================
  // Internal context
  // =========================

  private static final class HostExecutionContext {

    final ExecutorService executor;
    final RateLimiter rateLimiter;

    HostExecutionContext(
        ExecutorService executor,
        RateLimiter rateLimiter
    ) {
      this.executor = executor;
      this.rateLimiter = rateLimiter;
    }
  }
}
