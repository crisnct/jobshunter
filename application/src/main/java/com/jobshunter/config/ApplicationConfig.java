package com.jobshunter.config;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
@Slf4j
public class ApplicationConfig {

  @Bean
  public JsonMapper createMapper() {
    return JsonMapper.builder().findAndAddModules().build();
  }

  @Bean(name = "springSecurityMessageSource")
  public MessageSource springSecurityMessageSource() {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("org.springframework.security.messages");
    messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name()); // use platform default to suppress ISO-8859-1 log noise
    return messageSource;
  }

}
