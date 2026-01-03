package com.jobshunter.service.testdata;

import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.serpRequest.SearchWithSerpRequest;
import com.jobshunter.model.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.AiJobsClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("JobsClientSerp")
@ConditionalOnProperty(name = "serp.enabled", havingValue = "false")
@PackageExpected("com.jobshunter.service.clients.serp")
public non-sealed class FakeSerpClient implements AiJobsClient<SearchWithSerpRequest, List<Job>> {

  @Override
  @RateLimiter(name = "serpLimiter")
  @CircuitBreaker(name = "serp", fallbackMethod = "fallbackSearch")
  @Bulkhead(name = "serpBulkhead")
  public List<Job> searchJobs(SearchWithSerpRequest request) {
    return List.of(
        new Job(-1,
            "https://jobs.digitalhire.com/job-listing/opening/6W2b0Y7QrlHiOrwemePL8C?utm_campaign=google_jobs_apply&utm_source=google_jobs_apply&utm_medium=organic",
            null
        ),
        new Job(72,
            "https://www.accenture.com/us-en/careers/jobdetails?id=R00298524_en&title=SAP+Intercompany+Manager+-+Life+Sciences",
            null
        ),
        new Job(71,
            "https://www.linkedin.com/jobs/collections/recommended/?currentJobId=4263267426",
            null
        )
    );
  }

  @Override
  public List<CompanyDto> searchCompanies(SearchWithSerpRequest request) {
    return List.of();
  }

  @Override
  public List<Job> searchJobsFromCompanies(SearchWithSerpRequest request, List<CompanyDto> group) {
    return List.of();
  }

  @SuppressWarnings("unused")
  private List<Job> fallbackSearch(SearchWithSerpRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return List.of();
  }

}
