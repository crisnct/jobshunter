package com.jobshunter.service.clients;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.mailtrap.RestMailtrapClientImpl;
import com.jobshunter.testdata.DummyRestMailtrapClient;
import lombok.NonNull;

@PackageExpected("com.jobshunter.service.application.notifiers")
public sealed interface RestMailtrapClient permits RestMailtrapClientImpl, DummyRestMailtrapClient {

  void sendEmailWithNewJobs(
      @NonNull String username,
      @NonNull String email,
      @NonNull String body
  );

}
