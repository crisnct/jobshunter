package com.jobshunter.service.application.metrics;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Micrometer configuration for enabling @Timed annotations via AOP.
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class MicrometerConfig {

  /**
   * Creates the TimedAspect bean required for @Timed annotation support.
   * Without this bean, @Timed annotations will not record timing metrics.
   *
   * @param registry the meter registry to record metrics
   * @return the timed aspect
   */
  @Bean
  public TimedAspect timedAspect(MeterRegistry registry) {
    return new TimedAspect(registry);
  }
}
