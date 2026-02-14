package com.jobshunter.service.application.metrics;

import com.jobshunter.service.LimitedVirtualThreadExecutor;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import jakarta.annotation.Nonnull;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Binds LimitedVirtualThreadExecutor metrics to Micrometer registry.
 * Exposes gauges for active threads, queue size, completed tasks, and errors.
 */
@Component
public class ExecutorMetricsBinder implements MeterBinder {

  private final Map<String, LimitedVirtualThreadExecutor> executors;

  public ExecutorMetricsBinder(
      @Qualifier("gptSearchExecutor") LimitedVirtualThreadExecutor gptExecutor,
      @Qualifier("grokSearchExecutor") LimitedVirtualThreadExecutor grokExecutor,
      @Qualifier("geminiSearchExecutor") LimitedVirtualThreadExecutor geminiExecutor,
      @Qualifier("serpExecutor") LimitedVirtualThreadExecutor serpExecutor,
      @Qualifier("urlFetchRestClientExecutor") LimitedVirtualThreadExecutor restClientExecutor,
      @Qualifier("jobProcessingExecutor") LimitedVirtualThreadExecutor jobProcessingExecutor,
      @Qualifier("ordersExecutor") LimitedVirtualThreadExecutor ordersExecutor,
      @Qualifier("notificationsExecutor") LimitedVirtualThreadExecutor notificationsExecutor,
      @Qualifier("maintenanceExecutor") LimitedVirtualThreadExecutor maintenanceExecutor,
      @Qualifier("defaultAsyncExecutor") LimitedVirtualThreadExecutor defaultAsyncExecutor
  ) {
    this.executors = Map.ofEntries(
        Map.entry("gpt", gptExecutor),
        Map.entry("grok", grokExecutor),
        Map.entry("gemini", geminiExecutor),
        Map.entry("serp", serpExecutor),
        Map.entry("url-fetch-rest-client", restClientExecutor),
        Map.entry("job-processing", jobProcessingExecutor),
        Map.entry("order-processing", ordersExecutor),
        Map.entry("notifications", notificationsExecutor),
        Map.entry("maintenance", maintenanceExecutor),
        Map.entry("async", defaultAsyncExecutor)
    );
  }

  @Override
  public void bindTo(@Nonnull MeterRegistry registry) {
    executors.forEach((name, executor) -> {
      Gauge.builder("executor.active.threads", executor, LimitedVirtualThreadExecutor::getActiveThreads)
          .tag("name", name)
          .description("Number of currently active threads")
          .register(registry);

      Gauge.builder("executor.queue.size", executor, LimitedVirtualThreadExecutor::getQueue)
          .tag("name", name)
          .description("Number of tasks waiting in queue")
          .register(registry);

      Gauge.builder("executor.completed.total", executor, LimitedVirtualThreadExecutor::getCompletedTaskSuccessCount)
          .tag("name", name)
          .description("Total number of completed tasks")
          .register(registry);

      Gauge.builder("executor.errors.total", executor, LimitedVirtualThreadExecutor::getTasksWithErrors)
          .tag("name", name)
          .description("Total number of failed tasks")
          .register(registry);

      Gauge.builder("executor.max.threads", executor, LimitedVirtualThreadExecutor::getMaxThreads)
          .tag("name", name)
          .description("Maximum concurrent threads allowed")
          .register(registry);
    });
  }
}
