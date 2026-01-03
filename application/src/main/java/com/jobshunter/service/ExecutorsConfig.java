package com.jobshunter.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorsConfig {

  @Bean(name = "gptSearchExecutor")
  public ThreadPoolExecutor gptSearchExecutor(@Value("${gpt.threads:}") String threads) {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Integer.parseInt(threads));
    executor.setThreadFactory(Thread.ofVirtual().name("gpt-search-", 0).factory());
    return executor;
  }

  @Bean(name = "grokSearchExecutor")
  public ThreadPoolExecutor grokSearchExecutor(@Value("${grok.threads:}") String threads) {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Integer.parseInt(threads));
    executor.setThreadFactory(Thread.ofVirtual().name("grok-search-", 0).factory());
    return executor;
  }

  @Bean(name = "geminiSearchExecutor")
  public ThreadPoolExecutor geminiSearchExecutor(@Value("${gemini.threads:}") String threads) {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Integer.parseInt(threads));
    executor.setThreadFactory(Thread.ofVirtual().name("gemini-search-", 0).factory());
    return executor;
  }

  @Bean(name = "serpExecutor")
  public ThreadPoolExecutor serpExecutor(@Value("${serp.threads:}") String threads) {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Integer.parseInt(threads));
    executor.setThreadFactory(Thread.ofVirtual().name("serp-search-", 0).factory());
    return executor;
  }

  @Bean(name = "miscellaneousExecutor")
  public ThreadPoolExecutor miscellaneousExecutor(@Value("${jobshunter.miscellaneousExecutorThreads:}") String threads) {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Integer.parseInt(threads));
    executor.setThreadFactory(Thread.ofVirtual().name("miscellaneous-", 0).factory());
    return executor;
  }

}
