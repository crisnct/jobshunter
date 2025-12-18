package com.jobshunter.service.clients;

import com.jobshunter.dto.GptJobSearchRequest;
import com.jobshunter.dto.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.gpt.EconomyGptJobSearchImpl;
import com.jobshunter.testdata.DummyEconomyGpt;
import java.util.List;

@PackageExpected("com.jobshunter.service.application")
public sealed interface EconomyGptClient
    <T extends GptJobSearchRequest, F extends List<Job>> permits EconomyGptJobSearchImpl, DummyEconomyGpt {

  F searchJobs(T request);

}
