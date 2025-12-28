package com.jobshunter.service.clients;

import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.gemini.EconomyGeminiJobSearchImpl;
import com.jobshunter.service.clients.gemini.PremiumGeminiJobSearchImpl;
import com.jobshunter.service.clients.gpt.EconomyGptJobSearchImpl;
import com.jobshunter.service.clients.gpt.PremiumGptJobSearchImpl;
import com.jobshunter.service.clients.serpapi.SerpApiClientImpl;
import com.jobshunter.service.testdata.FakeGeminiEconomy;
import com.jobshunter.service.testdata.FakeGptEconomy;
import com.jobshunter.service.testdata.FakeGeminiPremium;
import com.jobshunter.service.testdata.FakeGptPremium;
import com.jobshunter.service.testdata.FakeSerpApiEconomyClient;
import java.util.List;

@PackageExpected("com.jobshunter.service.application")
public sealed interface AiJobsClient
    <T extends AIJobSearchRequest, F extends List<Job>> permits EconomyGeminiJobSearchImpl, PremiumGeminiJobSearchImpl, EconomyGptJobSearchImpl,
    PremiumGptJobSearchImpl, SerpApiClientImpl, FakeGeminiEconomy, FakeGptEconomy, FakeSerpApiEconomyClient, FakeGeminiPremium, FakeGptPremium {

  F searchJobs(T request);

}
