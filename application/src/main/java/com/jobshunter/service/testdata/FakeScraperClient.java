package com.jobshunter.service.testdata;

import com.jobshunter.dto.ScraperSearchRequest;
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
@Component("JobsClientScraper")
@PackageExpected("com.jobshunter.service.clients.scraper")
@ConditionalOnProperty(name = "scraper.enabled", havingValue = "false")
public non-sealed class FakeScraperClient implements AiJobsClient<ScraperSearchRequest> {

  @Override
  @CircuitBreaker(name = "scraperCircuitBreaker", fallbackMethod = "fallbackSearch")
  @RateLimiter(name = "scraperLimiter")
  @Bulkhead(name = "scraperBulkhead")
  public AiClientResponse searchJobs(ScraperSearchRequest request) {
    AiClientResponse result = new AiClientResponse();
    result.addAll(List.of(
        new Job(
            "https://www.bestjobs.eu/loc-de-munca/senior-it-engineer-for-ibm-elm-jazz-platform?rid=2436f233-fc47-4012-aae6-5ca0d7f8a35c&pos=1&selectedJobSlug=senior-it-engineer-for-ibm-elm-jazz-platform"
        ),
        new Job(
            "https://www.bestjobs.eu/loc-de-munca/system-tester-automotive-sensors?rid=eddbcd43-29f2-4051-82cd-1ce54f4ebd36&pos=25&selectedJobSlug=system-tester-automotive-sensors"
        ),
        new Job(
            "https://www.bestjobs.eu/loc-de-munca/service-system-analyst?rid=14c9807a-cc68-44ae-91e8-e4693af735d2&pos=23&selectedJobSlug=service-system-analyst"
        )
    ));
    return result;
  }

  @SuppressWarnings("unused")
  private AiClientResponse fallbackSearch(ScraperSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return new AiClientResponse();
  }

}
