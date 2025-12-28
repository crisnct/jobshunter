package com.jobshunter.service.clients;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.gemini.GeminiJobScoreCalculatorClientImpl;
import com.jobshunter.service.clients.gpt.GptJobScoreCalculatorClientImpl;
import com.jobshunter.service.testdata.FakeGeminiJobScoreCalculator;
import com.jobshunter.service.testdata.FakeGptJobScoreCalculator;

@PackageExpected("com.jobshunter.service.application")
public sealed interface JobScoreCalculatorClient<T extends JobScoreRequest>
    permits GeminiJobScoreCalculatorClientImpl, GptJobScoreCalculatorClientImpl, FakeGeminiJobScoreCalculator, FakeGptJobScoreCalculator {

  int computeScore(T request);

}
