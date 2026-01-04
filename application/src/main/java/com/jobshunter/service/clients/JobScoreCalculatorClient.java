package com.jobshunter.service.clients;

import com.jobshunter.model.JobScoreRequest;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.gemini.GeminiJobScoreCalculatorClientImpl;
import com.jobshunter.service.clients.gpt.GptJobScoreCalculatorClientImpl;
import com.jobshunter.service.clients.grok.GrokJobScoreCalculatorClientImpl;
import com.jobshunter.service.testdata.FakeGeminiJobScoreCalculator;
import com.jobshunter.service.testdata.FakeGptJobScoreCalculator;
import com.jobshunter.service.testdata.FakeGrokJobScoreCalculator;

@PackageExpected("com.jobshunter.service.application")
public sealed interface JobScoreCalculatorClient<T extends JobScoreRequest>
    permits GeminiJobScoreCalculatorClientImpl, GptJobScoreCalculatorClientImpl, GrokJobScoreCalculatorClientImpl, FakeGeminiJobScoreCalculator,
    FakeGptJobScoreCalculator, FakeGrokJobScoreCalculator {

  int computeScore(T request);

}
