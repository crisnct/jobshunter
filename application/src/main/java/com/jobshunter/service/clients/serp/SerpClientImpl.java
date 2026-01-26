package com.jobshunter.service.clients.serp;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.dto.serpResponse.SerpJobHit;
import com.jobshunter.dto.serpResponse.SerpJobsResult;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobMetadataType;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.browser.BrowserSimulator;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Read documentation here
 * <a href="https://serpapi.com/google-jobs-api">https://serpapi.com/google-jobs-api</a>
 */
@Slf4j
@Component("JobsClientSerp")
@ConditionalOnProperty(name = "serp.enabled", havingValue = "true")
@RequiredArgsConstructor
public non-sealed class SerpClientImpl implements AiJobsClient {

  private static final URI BASE = URI.create("https://serpapi.com/search");

  private final BrowserSimulator browserSimulator;

  private final ApplicationProperties applicationProperties;

  private static String encode(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

  @Override
  @RateLimiter(name = "serpLimiter")
  @CircuitBreaker(name = "serp", fallbackMethod = "fallbackSearch")
  @Bulkhead(name = "serpBulkhead")
  public AiClientResponse searchJobs(@NotNull AIJobSearchRequest request) {
    SerpJobsResult results;
    try {
      results = searchJobsPagination(request, null);
      for (int i = 0; i < applicationProperties.getSerp().getMaxPageSearch(); i++) {
        if (results.nextPageToken() == null) {
          break;
        } else {
          SerpJobsResult results2 = searchJobsPagination(request, results.nextPageToken());
          results = consolidate(results, results2);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    AiClientResponse response = new AiClientResponse();
    response.setId(results.id());
    for (SerpJobHit serpJob : results.jobs()) {
      if (serpJob.applyLinks().isEmpty()) {
        log.warn("Not found apply link from company {}", serpJob.company());
      } else {
        Job job = new Job(serpJob.applyLinks().getFirst());
        job.addMetadata(JobMetadataType.SERP_DESCRIPTION, serpJob.description() + "\n" + serpJob.highlights());
        response.add(job);
      }
    }
    return response;
  }

  @SuppressWarnings("unused")
  private AiClientResponse fallbackSearch(@NotNull AIJobSearchRequest request, Throwable t) {
    log.error("Serp search short-circuited/bulkheaded: {}", t.getMessage());
    return new AiClientResponse();
  }

  private SerpJobsResult consolidate(SerpJobsResult results1, SerpJobsResult results2) {
    List<SerpJobHit> jobs = new ArrayList<>(results1.jobs());
    jobs.addAll(results2.jobs());
    return new SerpJobsResult(results1.id(), jobs, results2.nextPageToken());
  }

  private SerpJobsResult searchJobsPagination(AIJobSearchRequest request, String nextPageToken) throws IOException {
    log.info("Searching jobs with Serp Api, query: {}", request.getUserPrompt());
    final URI uri = this.buildUri(request, nextPageToken);
    try {
      ResponseEntity<String> response = browserSimulator.openPageAsync(uri.toString()).toCompletableFuture().get();
      if (response.getStatusCode().is2xxSuccessful()) {
        log.info("SERP API request executed successfully");
      }
      if (response.getStatusCode().isError()) {
        throw new RuntimeException("SERP failed: " + response.getStatusCode().value() + " " + response.getBody());
      }
      return new SerpJobsResponseParser().parse(response.getBody());
    } catch (Throwable e) {
      log.error(e.getMessage());
      return new SerpJobsResult(null, List.of(), null);
    }
  }

  private URI buildUri(AIJobSearchRequest request, String nextPageToken) {
    List<String> parameters = new ArrayList<>();
    parameters.add("api_key");
    parameters.add(applicationProperties.getSerp().getApiKey());

    parameters.add("engine");
    parameters.add(request.getOrder().getModel().getModel().toLowerCase());

    parameters.add("q");
    parameters.add(encode(request.getUserPrompt()));

    parameters.add("chips");
    parameters.add("remote,employment_type:CONTRACTOR,date_posted:14days");

    //language of the Google interface
    parameters.add("hl");
    parameters.add("en");

    if (nextPageToken != null) {
      parameters.add("next_page_token");
      parameters.add(nextPageToken);
    }

    String[] kv = parameters.toArray(String[]::new);
    if (kv.length % 2 != 0) {
      throw new RuntimeException("key/value pairs required");
    }
    StringBuilder sb = new StringBuilder(BASE.toString()).append("?");
    for (int i = 0; i < kv.length; i += 2) {
      if (i > 0) {
        sb.append("&");
      }
      sb.append(kv[i]).append("=").append(kv[i + 1]);
    }
    return URI.create(sb.toString());
  }

}
