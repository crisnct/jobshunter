package com.jobshunter.service.clients;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.gpt.AbstractGptApiClient;
import com.jobshunter.service.clients.serpapi.SerpApiClient;

@PackageExpected("com.jobshunter.service.application")
public sealed interface JobSearchApiClient<T, F> permits AbstractGptApiClient, SerpApiClient {

  F searchJobs(T request);

}
