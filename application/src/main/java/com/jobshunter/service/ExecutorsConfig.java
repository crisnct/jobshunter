package com.jobshunter.service;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorsConfig {

  @Bean(name = "gptSearchExecutor")
  public Executor gptSearchExecutor() {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
    executor.setThreadFactory(Thread.ofVirtual().name("gpt-search-", 0).factory());
    return executor;
  }

  @Bean(name = "geminiSearchExecutor")
  public Executor geminiSearchExecutor() {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
    executor.setThreadFactory(Thread.ofVirtual().name("gemini-search-", 0).factory());
    return executor;
  }

  @Bean(name = "serpApiExecutor")
  public Executor serpApiExecutor() {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
    executor.setThreadFactory(Thread.ofVirtual().name("serp-search-", 0).factory());
    return executor;
  }

  @Bean(name = "jobsValidatorExecutor")
  public Executor jobsValidatorExecutor() {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
    executor.setThreadFactory(Thread.ofVirtual().name("jobs-validator-", 0).factory());
    return executor;
  }

}
