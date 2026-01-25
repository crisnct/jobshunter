package com.jobshunter.service.testdata;

import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.model.AiClientResponse;
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
@Component("JobsClientGemini")
@PackageExpected("com.jobshunter.service.clients.gemini")
@ConditionalOnProperty(name = "gemini.enabled", havingValue = "false")
public non-sealed class FakeGeminiClient implements AiJobsClient {

  @Override
  @CircuitBreaker(name = "geminiCircuitBreaker", fallbackMethod = "fallbackSearch")
  @RateLimiter(name = "geminiLimiter")
  @Bulkhead(name = "geminiBulkhead")
  public AiClientResponse searchJobs(AIJobSearchRequest request) {
    AiClientResponse result = new AiClientResponse();
    result.addAll(List.of(
        new Job(
            "https://devjob.ro/en/jobs/Evantage-Soft-SRL-Senior-Java-Full-Stack-Developer"
        ),
        new Job(
            "https://jobs.citi.com/job/pune/java-spark-senior-lead-developer-java-spark-hdfs-hive-vice-president/287/88303699072"
        ),
        new Job(
            "https://jobs.citi.com/job/dublin/technical-java-lead-vice-president/287/89302337248"
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
  private AiClientResponse fallbackSearch(AIJobSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return new AiClientResponse();
  }
}
