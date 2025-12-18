package com.jobshunter.service.clients;

import com.jobshunter.dto.SearchWithSerpRequest;
import com.jobshunter.dto.SerpApiJobsResult;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.serpapi.SerpApiClientImpl;
import com.jobshunter.testdata.DummySerpApiClient;

@PackageExpected("com.jobshunter.service.application")
public sealed interface SerpApiClient<T extends SearchWithSerpRequest, F extends SerpApiJobsResult>
    permits SerpApiClientImpl, DummySerpApiClient {

  F searchJobs(T request);
}


