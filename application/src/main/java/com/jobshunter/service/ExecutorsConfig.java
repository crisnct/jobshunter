package com.jobshunter.service;

import com.jobshunter.config.ApplicationProperties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@AllArgsConstructor
public class ExecutorsConfig implements AsyncConfigurer {

  private final ApplicationProperties properties;

  @Bean(name = "gptSearchExecutor")
  public LimitedVirtualThreadExecutor gptSearchExecutor() {
    return new LimitedVirtualThreadExecutor("gpt", properties.getGpt().getThreads());
  }

  @Bean(name = "grokSearchExecutor")
  public LimitedVirtualThreadExecutor grokSearchExecutor() {
    return new LimitedVirtualThreadExecutor("grok", properties.getGrok().getThreads());
  }

  @Bean(name = "geminiSearchExecutor")
  public LimitedVirtualThreadExecutor geminiSearchExecutor() {
    return new LimitedVirtualThreadExecutor("gemini", properties.getGemini().getThreads());
  }

  @Bean(name = "serpExecutor")
  public LimitedVirtualThreadExecutor serpExecutor() {
    return new LimitedVirtualThreadExecutor("serp", properties.getSerp().getThreads());
  }

  @Bean(name = "scraperExecutor")
  public LimitedVirtualThreadExecutor scraperExecutor() {
    return new LimitedVirtualThreadExecutor("scraper", properties.getScraper().getThreads());
  }

  @Bean(name = "urlFetchPlaywrightExecutor")
  public ExecutorService urlFetchPlaywrightExecutor() {
    return Executors.newFixedThreadPool(1);
  }

  @Bean(name = "urlFetchRestClientExecutor")
  public LimitedVirtualThreadExecutor urlFetchRestClientExecutor() {
    return new LimitedVirtualThreadExecutor("url-fetch-rest-client",
        properties.getJobsHunter().getThreads().getUrlFetchRestClient());
  }

  @Bean(name = "jobProcessingExecutor")
  public LimitedVirtualThreadExecutor jobProcessingExecutor() {
    return new LimitedVirtualThreadExecutor("job-processing",
        properties.getJobsHunter().getThreads().getJobProcessing());
  }

  @Bean(name = "ordersExecutor")
  public LimitedVirtualThreadExecutor ordersExecutor() {
    return new LimitedVirtualThreadExecutor("order-processing",
        properties.getJobsHunter().getThreads().getOrders());
  }

  @Bean(name = "notificationsExecutor")
  public LimitedVirtualThreadExecutor notificationsExecutor() {
    return new LimitedVirtualThreadExecutor("notifications-processing",
        properties.getJobsHunter().getThreads().getNotifications());
  }

  @Bean(name = "maintenanceExecutor")
  public LimitedVirtualThreadExecutor maintenanceExecutor() {
    return new LimitedVirtualThreadExecutor("maintenance",
        properties.getJobsHunter().getThreads().getMaintenance());
  }

  @Override
  public Executor getAsyncExecutor() {
    return defaultExecutor();
  }

  @Bean(name = "defaultAsyncExecutor")
  public LimitedVirtualThreadExecutor defaultExecutor() {
    return new LimitedVirtualThreadExecutor("async",
        properties.getJobsHunter().getThreads().getAsync());
  }

}
