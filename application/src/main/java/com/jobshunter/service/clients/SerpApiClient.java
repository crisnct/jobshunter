package com.jobshunter.service.clients;

import com.jobshunter.dto.serpRequest.SearchWithSerpRequest;
import com.jobshunter.dto.serpResponse.SerpApiJobsResult;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.serpapi.SerpApiClientImpl;
import com.jobshunter.testdata.DummySerpApiClient;

@PackageExpected("com.jobshunter.service.application")
public sealed interface SerpApiClient<T extends SearchWithSerpRequest, F extends SerpApiJobsResult>
    permits SerpApiClientImpl, DummySerpApiClient {

  F searchJobs(T request);
}


