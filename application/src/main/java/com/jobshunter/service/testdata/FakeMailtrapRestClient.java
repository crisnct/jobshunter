package com.jobshunter.service.testdata;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.RestMailtrapClient;
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
  public void sendEmailWithNewJobs(@NonNull String username, @NonNull String email, @NonNull String body) {
    log.info("Sending email to {}", username);
    log.info("Email send successfully to {}", username);
  }

}
