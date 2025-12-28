package com.jobshunter.service.clients.serpapi;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.dto.serpRequest.SearchWithSerpRequest;
import com.jobshunter.dto.serpResponse.SerpApiJobHit;
import com.jobshunter.dto.serpResponse.SerpApiJobsResult;
import com.jobshunter.model.Job;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.BrowserSimulator;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
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
@Component("EconomyJobsClientSerp")
@ConditionalOnProperty(name = "serpApi.enabled", havingValue = "true")
@RequiredArgsConstructor
public non-sealed class SerpApiClientImpl implements AiJobsClient<SearchWithSerpRequest, List<Job>> {

  private static final URI BASE = URI.create("https://serpapi.com/search");

  private static final double KM_TO_MILES = 0.621371;

  // Prefix standard Google pentru uule
  private static final String UULE_PREFIX = "w+CAIQICI";

  private final BrowserSimulator browserSimulator;

  private final ApplicationProperties applicationProperties;

  @Override
  @RateLimiter(name = "serpApiLimiter")
  @CircuitBreaker(name = "serpApi", fallbackMethod = "fallbackSearch")
  @Bulkhead(name = "serpApiBulkhead", type = Bulkhead.Type.SEMAPHORE)
  public List<Job> searchJobs(@NotNull SearchWithSerpRequest request) {
    SerpApiJobsResult results;
    try {
      results = searchJobsPagination(request, null);
      for (int i = 0; i < applicationProperties.getSerpApi().getMaxPageSearch(); i++) {
        if (results.nextPageToken() == null) {
          break;
        } else {
          SerpApiJobsResult results2 = searchJobsPagination(request, results.nextPageToken());
          results = consolidate(results, results2);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    final List<Job> jobs = new ArrayList<>();
    for (SerpApiJobHit serpJob : results.jobs()) {
      Job job = new Job(-1, serpJob.applyLinks().getFirst(), request.getPrompt().getEngineConfiguration().getModel());
      job.setDescription(serpJob.description() + "\n" + serpJob.highlights());
      jobs.add(job);
    }

    return jobs;
  }

  @SuppressWarnings("unused")
  private List<Job> fallbackSearch(@NotNull SearchWithSerpRequest request, Throwable t) {
    log.error("SerpApi search short-circuited/bulkheaded: {}", t.getMessage());
    return List.of();
  }

  private SerpApiJobsResult consolidate(SerpApiJobsResult results1, SerpApiJobsResult results2) {
    List<SerpApiJobHit> jobs = new ArrayList<>(results1.jobs());
    jobs.addAll(results2.jobs());
    return new SerpApiJobsResult(jobs, results2.nextPageToken());
  }

  private SerpApiJobsResult searchJobsPagination(SearchWithSerpRequest request, String nextPageToken) throws IOException {
    log.info("Searching jobs with Serp Api, query: {}", request.getQuery());
    final URI uri = this.buildUri(request, nextPageToken);
    try {
      ResponseEntity<String> response = browserSimulator.openPage(uri.toString()).toCompletableFuture().get();
      if (response.getStatusCode().is2xxSuccessful()) {
        log.info("SERP API request executed successfully");
      }
      if (response.getStatusCode().isError()) {
        throw new IllegalStateException("SERP API failed: " + response.getStatusCode().value() + " " + response.getBody());
      }
      return new SerpApiJobsResponseParser().parse(response.getBody());
    } catch (Throwable e) {
      log.error(e.getMessage());
      return new SerpApiJobsResult(List.of(), null);
    }
  }

  private URI buildUri(SearchWithSerpRequest request, String nextPageToken) {
    List<String> parameters = new ArrayList<>();
    parameters.add("api_key");
    parameters.add(applicationProperties.getSerpApi().getApiKey());
    parameters.add("engine");
    parameters.add(request.getPrompt().getEngineConfiguration().getModel());
    parameters.add("q");
    parameters.add(encode(request.getQuery()));

    if (request.getLocation() != null) {
      parameters.add("uule");
      parameters.add(encodeLocation(request.getLocation()));
      if (request.getRadius() != null) {
        parameters.add("lrad");
        parameters.add(String.valueOf(kilometersToMiles(request.getRadius())));
      }
    }
    if (request.getWorkType() != null) {
      parameters.add("ltype");
      String value = switch (request.getWorkType()){
        case ONSITE -> "1";
        case REMOTE -> "2";
        case HYBRID -> "3";
      };
      parameters.add(value);
    }

    parameters.add("chips");
    if (request.getDatePosted() == null) {
      parameters.add("date_posted:last_3_days");
    } else {
      parameters.add("date_posted:" + request.getDatePosted());
    }

    if (request.getGoogleDomain() != null) {
      parameters.add("google_domain");
      parameters.add(request.getGoogleDomain());
    }
    if (request.getLanguage() != null) {
      parameters.add("hl");
      parameters.add(request.getLanguage());
    }
    if (request.getCountry() != null) {
      parameters.add("gl");
      parameters.add(request.getCountry());
    }
    if (nextPageToken != null) {
      parameters.add("next_page_token");
      parameters.add(nextPageToken);
    }

    String[] kv = parameters.toArray(String[]::new);
    if (kv.length % 2 != 0) {
      throw new IllegalArgumentException("key/value pairs required");
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

  private static String encode(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

  private String encodeLocation(@NotBlank String location) {
    byte[] utf8Bytes = location.getBytes(StandardCharsets.UTF_8);
    String base64 = Base64.getEncoder().encodeToString(utf8Bytes);
    return UULE_PREFIX + base64;
  }

  private double kilometersToMiles(double kilometers) {
    if (kilometers < 0) {
      throw new IllegalArgumentException("Distance cannot be negative");
    }
    return kilometers * KM_TO_MILES;
  }

}
