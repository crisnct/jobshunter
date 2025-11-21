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
        .build();
  }

  @Bean
  public JsonMapper createMapper(){
    return JsonMapper.builder().findAndAddModules().build();
  }

}
