package com.jobshunter.service.clients;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.mailtrap.SmtpMailtrapClientImpl;
import com.jobshunter.testdata.DummySmtpMailtrapClient;
import jakarta.annotation.Nullable;
import java.util.List;
import lombok.NonNull;
import org.springframework.web.multipart.MultipartFile;

@PackageExpected("com.jobshunter.service.application.notifiers")
public sealed interface SmtpMailtrapClient permits SmtpMailtrapClientImpl, DummySmtpMailtrapClient {

  void sendEmail(
      @NonNull List<String> to,
      @Nullable String subject,
      @NonNull String body,
      @Nullable MultipartFile attachment
  );

}
