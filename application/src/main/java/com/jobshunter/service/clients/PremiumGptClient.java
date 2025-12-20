package com.jobshunter.service.clients;

import com.jobshunter.dto.gptRequest.GptJobSearchRequest;
import com.jobshunter.dto.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.gpt.PremiumGptJobSearchImpl;
import com.jobshunter.testdata.DummyPremiumGpt;
import java.util.List;

@PackageExpected("com.jobshunter.service.application")
public sealed interface PremiumGptClient<T extends GptJobSearchRequest, F extends List<Job>>
    permits PremiumGptJobSearchImpl, DummyPremiumGpt {

  F searchJobs(T request);
}
