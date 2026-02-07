package com.jobshunter.service.testdata;

import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.GptSearchRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.AiJobsCompaniesClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("JobsClientGPT")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "false")
public non-sealed class FakeGptClient implements AiJobsClient<GptSearchRequest>, AiJobsCompaniesClient<GptSearchRequest> {

  @Override
  @CircuitBreaker(name = "gptCircuitBreaker", fallbackMethod = "fallbackSearch")
  @RateLimiter(name = "gptLimiter")
  @Bulkhead(name = "gptBulkhead")
  public AiClientResponse searchJobs(GptSearchRequest request) {
    AiClientResponse result = new AiClientResponse();
    result.addAll(List.of(
        new Job(
            "https://br.bebee.com/job/63c331e10c2e5c04df61d25ef8219be8?utm_campaign=google_jobs_apply&utm_source=google_jobs_apply&utm_medium=organic"
        ),
        new Job(
            "https://www.dice.com/job-detail/1f3c5759-dfad-40d7-9e0a-aa6fdd24db5c?utm_source=openai"
        ),
        new Job(
            "https://www.linkedin.com/jobs/collections/recommended/?currentJobId=42955246261"
        ),
        new Job(
            "https://weworkremotely.com/remote-jobs/h2corporation-vice-president-of-engineering-usa"
        ),
        new Job(
            "https://weworkremotely.com/remote-jobs/h2corporation-vice-president-of-engineering-usa"
        )
    ));
    return result;
  }

  @SuppressWarnings("unused")
  private AiClientResponse fallbackSearch(GptSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return new AiClientResponse();
  }

  @Override
  public List<CompanyDto> searchCompanies(GptSearchRequest request) {
    return List.of();
  }

  @Override
  public AiClientResponse searchJobsFromCompanies(GptSearchRequest request) {
    return null;
  }
}
