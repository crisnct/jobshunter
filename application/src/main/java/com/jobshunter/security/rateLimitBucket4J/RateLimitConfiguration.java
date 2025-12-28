package com.jobshunter.security.rateLimitBucket4J;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@formatter:off
/**
 * Centralized rate limiting configuration.
 */
//@formatter:on
@Configuration
public class RateLimitConfiguration {

  @Bean
  public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(RateLimitingFilter filter) {
    FilterRegistrationBean<RateLimitingFilter> bean = new FilterRegistrationBean<>(filter);
    bean.setOrder(-100); // early
    return bean;
  }

}