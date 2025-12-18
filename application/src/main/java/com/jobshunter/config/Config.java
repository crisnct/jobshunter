package com.jobshunter.config;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class Config {

  /// To be removed
  @Deprecated
  @Bean
  public RestTemplate restTemplate() {
    // request factory cu timeout-uri
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000);
    factory.setReadTimeout(15000);

    var restTemplate = new RestTemplate(factory);
    // interceptor exact ca în Spring Boot 3
    restTemplate.getInterceptors().add((request, body, execution) -> {
      var headers = request.getHeaders();
      headers.set("User-Agent", "Mozilla/5.0");
      headers.set("Accept-Language", "en-US,en;q=0.9");
      headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
      headers.set("Referer", "https://www.jobs-hunter.com");

      return execution.execute(request, body);
    });

    return restTemplate;
  }

  @Bean
  public JsonMapper createMapper() {
    return JsonMapper.builder().findAndAddModules().build();
  }

  @Bean(name = "gptSearchExecutor")
  public Executor gptSearchExecutor() {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
    executor.setThreadFactory(Thread.ofVirtual().name("gpt-search-", 0).factory());
    return executor;
  }

}
