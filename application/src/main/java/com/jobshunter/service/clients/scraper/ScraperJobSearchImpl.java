package com.jobshunter.service.clients.scraper;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.dto.ScraperSearchRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.UrlExtractor;
import com.jobshunter.service.application.cost.AiCostPublisher;
import com.jobshunter.service.application.cost.TokenEstimationGuard;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.browser.BrowserSimulator;
import com.jobshunter.service.retry.RetryPolicies;
import com.jobshunter.service.retry.RetryTemplate;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("JobsClientScraper")
@PackageExpected("com.jobshunter.service.clients.scraper")
@ConditionalOnProperty(name = "scraper.enabled", havingValue = "true")
@AllArgsConstructor
public non-sealed class ScraperJobSearchImpl implements AiJobsClient<ScraperSearchRequest> {

  private final BrowserSimulator browserSimulator;

  private final ApplicationProperties properties;

  private final RestClient restClient;

  private final RetryTemplate retryTemplate;

  private final JsonMapper mapper;

  private final UrlExtractor urlExtractor;

  private final TemplateRenderer templateRenderer;

  private final TokenEstimationGuard tokenEstimationGuard;

  private final AiCostPublisher costPublisher;

  @Override
  @CircuitBreaker(name = "scraperCircuitBreaker", fallbackMethod = "fallbackSearch")
  @RateLimiter(name = "scraperLimiter")
  @Bulkhead(name = "scraperBulkhead")
  public AiClientResponse searchJobs(ScraperSearchRequest request) {
    return retryTemplate.execute(RetryPolicies.JOB_SEARCH, "SCRAPER", () -> searchJobsOnce(request));
  }

  private AiClientResponse searchJobsOnce(ScraperSearchRequest request) {
    //TODO
    return new AiClientResponse();
  }

  @SuppressWarnings("unused")
  private AiClientResponse fallbackSearch(ScraperSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return new AiClientResponse();
  }

}
