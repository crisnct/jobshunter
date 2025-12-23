package com.jobshunter.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

  @Bean
  public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
    FilterRegistrationBean<RateLimitFilter> bean = new FilterRegistrationBean<>();
    bean.setFilter(new RateLimitFilter());
    bean.setOrder(1); // cât mai devreme
    return bean;
  }
}
