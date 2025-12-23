package com.jobshunter.service.clients;

import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.dto.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.gemini.EconomyGeminiJobSearchImpl;
import com.jobshunter.service.clients.gpt.EconomyGptJobSearchImpl;
import com.jobshunter.service.clients.gpt.PremiumGptJobSearchImpl;
import com.jobshunter.testdata.DummyEconomyGemini;
import com.jobshunter.testdata.DummyEconomyGpt;
import com.jobshunter.testdata.DummyPremiumGpt;
import java.util.List;

@PackageExpected("com.jobshunter.service.application")
public sealed interface AiJobsClient
    <T extends AIJobSearchRequest, F extends List<Job>> permits EconomyGeminiJobSearchImpl, EconomyGptJobSearchImpl, PremiumGptJobSearchImpl,
    DummyEconomyGemini, DummyEconomyGpt, DummyPremiumGpt {

  F searchJobs(T request);

}
