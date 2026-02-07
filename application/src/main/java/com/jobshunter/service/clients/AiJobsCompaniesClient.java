package com.jobshunter.service.clients;

import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.JobSearchRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.gemini.GeminiV1JobSearchImpl;
import com.jobshunter.service.clients.gpt.GptV1JobSearchImpl;
import com.jobshunter.service.clients.grok.GrokV1JobSearchImpl;
import com.jobshunter.service.testdata.FakeGeminiClient;
import com.jobshunter.service.testdata.FakeGptClient;
import com.jobshunter.service.testdata.FakeGrokClient;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@PackageExpected("com.jobshunter.service.application")
public sealed interface AiJobsCompaniesClient<R extends JobSearchRequest>
    permits GeminiV1JobSearchImpl, GptV1JobSearchImpl, GrokV1JobSearchImpl,
    FakeGeminiClient, FakeGptClient, FakeGrokClient {

  @NotNull
  List<CompanyDto> searchCompanies(R request);

  @NotNull
  AiClientResponse searchJobsFromCompanies(R request);
}
