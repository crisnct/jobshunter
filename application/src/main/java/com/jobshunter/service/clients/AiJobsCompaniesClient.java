package com.jobshunter.service.clients;

import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.gpt.GptV1JobSearchImpl;
import com.jobshunter.service.clients.grok.GrokV1JobSearchImpl;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@PackageExpected("com.jobshunter.service.application")
public sealed interface AiJobsCompaniesClient permits GptV1JobSearchImpl, GrokV1JobSearchImpl {

  @NotNull
  List<CompanyDto> searchCompanies(AIJobSearchRequest request);

  @NotNull
  AiClientResponse searchJobsFromCompanies(AIJobSearchRequest request);
}
