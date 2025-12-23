package com.jobshunter.service.clients;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.gpt.GptJobScoreCalculatorClientImpl;
import com.jobshunter.service.testdata.DummyGptJobScoreCalculatorClient;

@PackageExpected("com.jobshunter.service.application")
public sealed interface GptJobScoreCalculatorClient permits GptJobScoreCalculatorClientImpl, DummyGptJobScoreCalculatorClient {

  int computeScore(String jobDescription, String fileId);

}
