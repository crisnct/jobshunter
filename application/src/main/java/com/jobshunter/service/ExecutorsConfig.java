package com.jobshunter.service;

import com.jobshunter.ApplicationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorsConfig {

  @Bean(name = "gptSearchExecutor")
  public LimitedVirtualThreadExecutor gptSearchExecutor(ApplicationProperties properties) {
    return new LimitedVirtualThreadExecutor("gpt",
        properties.getGpt().getThreads());
  }

  @Bean(name = "grokSearchExecutor")
  public LimitedVirtualThreadExecutor grokSearchExecutor(ApplicationProperties properties) {
    return new LimitedVirtualThreadExecutor("grok",
        properties.getGrok().getThreads());
  }

  @Bean(name = "geminiSearchExecutor")
  public LimitedVirtualThreadExecutor geminiSearchExecutor(ApplicationProperties properties) {
    return new LimitedVirtualThreadExecutor("gemini",
        properties.getGemini().getThreads());
  }

  @Bean(name = "serpExecutor")
  public LimitedVirtualThreadExecutor serpExecutor(ApplicationProperties properties) {
    return new LimitedVirtualThreadExecutor("serp",
        properties.getJobsHunter().getThreads().getUrlFetchPlaywright());
  }

  @Bean(name = "urlFetchPlaywrightExecutor")
  public LimitedVirtualThreadExecutor urlFetchPlaywrightExecutor(ApplicationProperties properties) {
    return new LimitedVirtualThreadExecutor("url-fetch-playwright",
        properties.getJobsHunter().getThreads().getUrlFetchPlaywright());
  }

  @Bean(name = "urlFetchRestClientExecutor")
  public LimitedVirtualThreadExecutor urlFetchRestClientExecutor(ApplicationProperties properties) {
    return new LimitedVirtualThreadExecutor("url-fetch-rest-client",
        properties.getJobsHunter().getThreads().getUrlFetchRestClient());
  }

  @Bean(name = "jobProcessingExecutor")
  public LimitedVirtualThreadExecutor jobProcessingExecutor(ApplicationProperties properties) {
    return new LimitedVirtualThreadExecutor("job-processing",
        properties.getJobsHunter().getThreads().getJobProcessing());
  }

  @Bean(name = "ordersExecutor")
  public LimitedVirtualThreadExecutor ordersExecutor(ApplicationProperties properties) {
    return new LimitedVirtualThreadExecutor("order-processing",
        properties.getJobsHunter().getThreads().getOrders());
  }

  @Bean(name = "notificationsExecutor")
  public LimitedVirtualThreadExecutor notificationsExecutor(ApplicationProperties properties) {
    return new LimitedVirtualThreadExecutor("notifications-processing",
        properties.getJobsHunter().getThreads().getNotifications());
  }

  @Bean(name = "maintenanceExecutor")
  public LimitedVirtualThreadExecutor maintenanceExecutor(ApplicationProperties properties) {
    return new LimitedVirtualThreadExecutor("maintenance",
        properties.getJobsHunter().getThreads().getMaintenance());
  }

}
