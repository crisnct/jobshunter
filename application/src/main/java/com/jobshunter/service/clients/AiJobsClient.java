package com.jobshunter.service.clients;

import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.gemini.GeminiV1JobSearchImpl;
import com.jobshunter.service.clients.gpt.GptV1JobSearchImpl;
import com.jobshunter.service.clients.grok.GrokV1JobSearchImpl;
import com.jobshunter.service.clients.serp.SerpClientImpl;
import com.jobshunter.service.testdata.FakeGeminiClient;
import com.jobshunter.service.testdata.FakeGptClient;
import com.jobshunter.service.testdata.FakeGrokClient;
import com.jobshunter.service.testdata.FakeSerpClient;
import jakarta.validation.constraints.NotNull;

@PackageExpected("com.jobshunter.service.application")
public sealed interface AiJobsClient
    permits GeminiV1JobSearchImpl, GptV1JobSearchImpl, GrokV1JobSearchImpl, SerpClientImpl,
    FakeGeminiClient, FakeGptClient, FakeGrokClient, FakeSerpClient {

  String REASONING_JOB_SEARCH = "low";

  @NotNull
  AiClientResponse searchJobs(AIJobSearchRequest request);

}
