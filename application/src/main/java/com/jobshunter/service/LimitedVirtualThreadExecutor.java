package com.jobshunter.service;

import jakarta.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.MDC;

/**
 * Executor implementation based on virtual threads with explicit concurrency limiting.
 * <p>
 * This executor is designed to be used directly with CompletableFuture Async methods.
 */
public final class LimitedVirtualThreadExecutor implements Executor {

  private final Executor delegate;
  private final Semaphore semaphore;
  private final AtomicInteger runningTasks = new AtomicInteger();
  private final AtomicInteger completedTasks = new AtomicInteger();
  private final AtomicInteger errorsTasks = new AtomicInteger();
  private final AtomicInteger queueTasks = new AtomicInteger();

  public LimitedVirtualThreadExecutor(String threadNamePrefix, int maxConcurrentTasks) {
    Objects.requireNonNull(threadNamePrefix, "threadNamePrefix");

    if (maxConcurrentTasks <= 0) {
      throw new IllegalArgumentException("maxConcurrentTasks must be > 0");
    }

    this.delegate = Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual().name(threadNamePrefix, 0).factory()
    );
    this.semaphore = new Semaphore(maxConcurrentTasks);
  }

  @Override
  public void execute(@Nonnull Runnable command) {
    queueTasks.incrementAndGet();
    // Capture MDC context for propagation to worker thread
    Map<String, String> mdcContext = MDC.getCopyOfContextMap();
    delegate.execute(() -> {
      semaphore.acquireUninterruptibly();
      queueTasks.decrementAndGet();
      runningTasks.incrementAndGet();
      // Restore MDC context for the entire duration of task execution
      if (mdcContext != null) {
        MDC.setContextMap(mdcContext);
      }
      try {
        command.run();
      } catch (Throwable e) {
        errorsTasks.incrementAndGet();
        throw e;
      } finally {
        MDC.clear();
        completedTasks.incrementAndGet();
        runningTasks.decrementAndGet();
        semaphore.release();
      }
    });
  }

  /**
   * Number of tasks currently executing (for metrics / debugging).
   */
  public int getActiveThreads() {
    return runningTasks.get();
  }

  /**
   * Configured concurrency limit.
   */
  public int getMaxThreads() {
    return semaphore.availablePermits() + runningTasks.get();
  }

  public int getCompletedTaskSuccessCount() {
    return completedTasks.get();
  }

  public int getTasksWithErrors() {
    return errorsTasks.get();
  }

  public int getQueue() {
    return queueTasks.get();
  }

}
