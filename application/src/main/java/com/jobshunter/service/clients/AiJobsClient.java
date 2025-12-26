package com.jobshunter.service.clients;

import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.gemini.EconomyGeminiJobSearchImpl;
import com.jobshunter.service.clients.gemini.PremiumGeminiJobSearchImpl;
import com.jobshunter.service.clients.gpt.EconomyGptJobSearchImpl;
import com.jobshunter.service.clients.gpt.PremiumGptJobSearchImpl;
import com.jobshunter.service.clients.serpapi.SerpApiClientImpl;
import com.jobshunter.service.testdata.DummyEconomyGemini;
import com.jobshunter.service.testdata.DummyEconomyGpt;
import com.jobshunter.service.testdata.DummyPremiumGemini;
import com.jobshunter.service.testdata.DummyPremiumGpt;
import com.jobshunter.service.testdata.DummyEconomySerpApiClient;
import java.util.List;

@PackageExpected("com.jobshunter.service.application")
public sealed interface AiJobsClient
    <T extends AIJobSearchRequest, F extends List<Job>> permits EconomyGeminiJobSearchImpl, PremiumGeminiJobSearchImpl, EconomyGptJobSearchImpl,
    PremiumGptJobSearchImpl, SerpApiClientImpl, DummyEconomyGemini, DummyEconomyGpt, DummyEconomySerpApiClient, DummyPremiumGemini, DummyPremiumGpt {

  F searchJobs(T request);

}
