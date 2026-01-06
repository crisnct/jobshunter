package com.jobshunter;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.security.JHHeaders;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

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
      headers.set(JHHeaders.USER_AGENT, "Mozilla/5.0");
      headers.set(JHHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9");
      headers.set(JHHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
      headers.set(JHHeaders.REFERER, "https://www.jobs-hunter.com");

      return execution.execute(request, body);
    });

    return restTemplate;
  }
}
