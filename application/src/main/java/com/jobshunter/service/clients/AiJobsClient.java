package com.jobshunter.service.clients;

import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.dto.CompanyDto;
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
import java.util.List;

@PackageExpected("com.jobshunter.service.application")
public sealed interface AiJobsClient
    <T extends AIJobSearchRequest, F extends AiClientResponse> permits GeminiV1JobSearchImpl, GptV1JobSearchImpl, GrokV1JobSearchImpl, SerpClientImpl,
    FakeGeminiClient, FakeGptClient, FakeGrokClient, FakeSerpClient {

  F searchJobs(T request);

  List<CompanyDto> searchCompanies(T request);

  F searchJobsFromCompanies(T request, List<CompanyDto> group);

}
