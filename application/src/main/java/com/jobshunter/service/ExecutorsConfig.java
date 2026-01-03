package com.jobshunter.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorsConfig {

  @Bean(name = "gptSearchExecutor")
  public ThreadPoolExecutor gptSearchExecutor() {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
    executor.setThreadFactory(Thread.ofVirtual().name("gpt-search-", 0).factory());
    return executor;
  }

  @Bean(name = "grokSearchExecutor")
  public ThreadPoolExecutor grokSearchExecutor() {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
    executor.setThreadFactory(Thread.ofVirtual().name("grok-search-", 0).factory());
    return executor;
  }

  @Bean(name = "geminiSearchExecutor")
  public ThreadPoolExecutor geminiSearchExecutor() {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
    executor.setThreadFactory(Thread.ofVirtual().name("gemini-search-", 0).factory());
    return executor;
  }

  @Bean(name = "serpApiExecutor")
  public ThreadPoolExecutor serpApiExecutor() {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
    executor.setThreadFactory(Thread.ofVirtual().name("serp-search-", 0).factory());
    return executor;
  }

  @Bean(name = "miscellaneousExecutor")
  public ThreadPoolExecutor miscellaneousExecutor() {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);
    executor.setThreadFactory(Thread.ofVirtual().name("miscellaneous-", 0).factory());
    return executor;
  }

}
