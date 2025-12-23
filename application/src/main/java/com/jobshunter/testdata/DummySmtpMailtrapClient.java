package com.jobshunter.testdata;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.SmtpMailtrapClient;
import jakarta.annotation.Nullable;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@PackageExpected("com.jobshunter.service.application.notifiers")
@ConditionalOnProperty(name = "spring.mail.enabled", havingValue = "false")
public final class DummySmtpMailtrapClient implements SmtpMailtrapClient {

  @Override
  public void sendEmail(
      @NonNull String to,
      @Nullable String subject,
      @NonNull String body
  ) {
    log.info("Sending email to {}", to);
    log.info("Email send successfully to {}", to);
  }

  @Override
  public void sendEmail(
      @NonNull List<String> to,
      @Nullable String subject,
      @NonNull String body,
      @Nullable MultipartFile attachment
  ) {
    log.info("Sending email with attachement to {}", to);
    log.info("Email send with attachement  successfully to {}", to);
  }

}


