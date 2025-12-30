package com.jobshunter.service.testdata;

import com.jobshunter.model.GptJobSearchRequest;
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
@Component("JobsClientGPT")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "false")
public non-sealed class FakeGptClient implements AiJobsClient<GptJobSearchRequest, List<Job>> {

  @Override
  @RateLimiter(name = "gptLimiter")
  @CircuitBreaker(name = "gptCircuitBreaker", fallbackMethod = "fallbackSearch")
  @Bulkhead(name = "gptBulkhead")
  public List<Job> searchJobs(GptJobSearchRequest request) {
    return List.of(
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
    );
  }

  @SuppressWarnings("unused")
  private List<Job> fallbackSearch(GptJobSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return List.of();
  }

}
