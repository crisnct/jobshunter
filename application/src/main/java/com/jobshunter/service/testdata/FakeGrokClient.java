package com.jobshunter.service.testdata;

import com.jobshunter.dto.CompanyDto;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.GrokJobSearchRequest;
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
@Component("JobsClientGROK")
@PackageExpected("com.jobshunter.service.clients.grok")
@ConditionalOnProperty(name = "grok.enabled", havingValue = "false")
public non-sealed class FakeGrokClient implements AiJobsClient<GrokJobSearchRequest, AiClientResponse> {

  @Override
  @CircuitBreaker(name = "grokCircuitBreaker", fallbackMethod = "fallbackSearch")
  @RateLimiter(name = "grokLimiter")
  @Bulkhead(name = "grokBulkhead")
  public AiClientResponse searchJobs(GrokJobSearchRequest request) {
    AiClientResponse result = new AiClientResponse();
    result.addAll(List.of(
        new Job(-1,
            "https://br.bebee.com/job/63c331e10c2e5c04df61d25ef8219be8?utm_campaign=google_jobs_apply&utm_source=google_jobs_apply&utm_medium=organic",
            null
        ),
        new Job(-1,
            "https://www.dice.com/job-detail/1f3c5759-dfad-40d7-9e0a-aa6fdd24db5c?utm_source=openai",
            null
        ),
        new Job(-1,
            "https://www.linkedin.com/jobs/collections/recommended/?currentJobId=42955246261",
            null
        ),
        new Job(-1,
            "https://weworkremotely.com/remote-jobs/h2corporation-vice-president-of-engineering-usa",
            null
        ),
        new Job(-1,
            "https://weworkremotely.com/remote-jobs/h2corporation-vice-president-of-engineering-usa",
            null
        )
    ));
    return result;
  }

  @Override
  @RateLimiter(name = "grokLimiter")
  @Bulkhead(name = "grokBulkhead")
  public List<CompanyDto> searchCompanies(GrokJobSearchRequest request) {
    return List.of();
  }

  @Override
  @RateLimiter(name = "grokLimiter")
  @Bulkhead(name = "grokBulkhead")
  public AiClientResponse searchJobsFromCompanies(GrokJobSearchRequest request, List<CompanyDto> group) {
    return new AiClientResponse();
  }

  @SuppressWarnings("unused")
  private AiClientResponse fallbackSearch(GrokJobSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return new AiClientResponse();
  }

}
