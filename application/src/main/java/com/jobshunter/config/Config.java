package com.jobshunter.config;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class Config {

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .setConnectTimeout(Duration.ofSeconds(5))
        .setReadTimeout(Duration.ofSeconds(15))
        .additionalInterceptors((request, body, execution) -> {
          request.getHeaders().set("User-Agent", "Mozilla/5.0");
          request.getHeaders().set("Accept-Language", "en-US,en;q=0.9");
          request.getHeaders().set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
          // Set a referer if you have a meaningful source URL:
          request.getHeaders().set("Referer", "https://www.jobs-hunter.com");
          return execution.execute(request, body);
        })
        .build();
  }

  @Bean
  public JsonMapper createMapper(){
    return JsonMapper.builder().findAndAddModules().build();
  }

}
