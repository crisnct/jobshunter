package com.jobshunter.service.testdata;

import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.PerplexitySearchRequest;
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

/**
 * Deterministic stand-in for the Perplexity client, active when {@code perplexity.enabled=false} (profile {@code local}), so the whole pipeline
 * runs without an API key or cost.
 */
@Slf4j
@Component("JobsClientPERPLEXITY")
@PackageExpected("com.jobshunter.service.clients.perplexity")
@ConditionalOnProperty(name = "perplexity.enabled", havingValue = "false")
public non-sealed class FakePerplexityClient implements AiJobsClient<PerplexitySearchRequest>, AiJobsCompaniesClient<PerplexitySearchRequest> {

  private static final List<String> JOB_URLS = List.of(
      "https://jobs.smartrecruiters.com/Endava/744000092939376",
      "https://careers.wipro.com/job/Java-Developer/117108-en_US/",
      "https://devjob.ro/en/jobs/CTP-GROUP-Senior-Java-Developer",
      "https://remotive.com/remote/jobs/software-development/senior-java-api-developer-3284334",
      "https://www.linkedin.com/jobs/view/senior-full-stack-java-developers-remote-at-the-dignify-solutions-llc-4351107084",
      "https://jobs.siemens.com/en_US/externaljobs/JobDetail/480292",
      "https://www.infosys.com/404"
  );

  @Override
  @CircuitBreaker(name = "perplexityCircuitBreaker", fallbackMethod = "fallbackSearch")
  @RateLimiter(name = "perplexityLimiter")
  @Bulkhead(name = "perplexityBulkhead")
  public AiClientResponse searchJobs(PerplexitySearchRequest request) {
    AiClientResponse result = new AiClientResponse();
    JOB_URLS.forEach(url -> result.add(new Job(url)));
    return result;
  }

  @SuppressWarnings("unused")
  private AiClientResponse fallbackSearch(PerplexitySearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return new AiClientResponse();
  }

  @Override
  public List<CompanyDto> searchCompanies(PerplexitySearchRequest request) {
    return List.of();
  }

  @Override
  public AiClientResponse searchJobsFromCompanies(PerplexitySearchRequest request) {
    return new AiClientResponse();
  }
}
