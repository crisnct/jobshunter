package com.jobshunter.service.clients;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.twilio.TwilioClientImpl;
import com.jobshunter.testdata.DummyTwilioClient;
import jakarta.validation.constraints.NotBlank;

@PackageExpected("com.jobshunter.service.application")
public sealed interface TwilioClient permits TwilioClientImpl, DummyTwilioClient {

  boolean trySend(
      @NotBlank String toNumber,
      @NotBlank String fromNumber,
      @NotBlank String body
  );

}
