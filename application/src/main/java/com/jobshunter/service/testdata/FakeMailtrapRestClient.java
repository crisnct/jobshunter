package com.jobshunter.service.testdata;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.RestMailtrapClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@PackageExpected("com.jobshunter.service.application.notifiers")
@ConditionalOnProperty(name = "spring.mail.enabled", havingValue = "false")
public non-sealed class FakeMailtrapRestClient implements RestMailtrapClient {

  @Override
  @RateLimiter(name = "mailtrapLimiter")
  @CircuitBreaker(name = "mailtrap", fallbackMethod = "fallbackSendEmail")
  @Bulkhead(name = "mailtrapBulkhead")
  public void sendEmailWithNewJobs(@NonNull String username, @NonNull String email, @NonNull String body) {
    log.info("Sending email to {}", username);
    log.info("Email send successfully to {}", username);
  }

  @SuppressWarnings("unused")
  private void fallbackSendEmail(String username, String email, String body, Throwable throwable) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), throwable.getMessage());
  }

}
