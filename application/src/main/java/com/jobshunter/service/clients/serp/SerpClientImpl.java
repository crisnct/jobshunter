package com.jobshunter.service.clients.serp;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.serpRequest.SearchWithSerpRequest;
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
@Component("JobsClientSerp")
@ConditionalOnProperty(name = "serp.enabled", havingValue = "true")
@RequiredArgsConstructor
public non-sealed class SerpClientImpl implements AiJobsClient<SearchWithSerpRequest, AiClientResponse> {

  private static final URI BASE = URI.create("https://serpapi.com/search");

  private static final double KM_TO_MILES = 0.621371;

  // Prefix standard Google pentru uule
  private static final String UULE_PREFIX = "w+CAIQICI";

  private final BrowserSimulator browserSimulator;

  private final ApplicationProperties applicationProperties;

  private static String encode(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

  @Override
  @RateLimiter(name = "serpLimiter")
  @CircuitBreaker(name = "serp", fallbackMethod = "fallbackSearch")
  @Bulkhead(name = "serpBulkhead")
  public AiClientResponse searchJobs(@NotNull SearchWithSerpRequest request) {
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
      Job job = new Job(-1, serpJob.applyLinks().getFirst(), request.getModel().getModel());
      job.addMetadata(JobMetadataType.SERP_DESCRIPTION, serpJob.description() + "\n" + serpJob.highlights());
      response.add(job);
    }
    return response;
  }

  @Override
  public List<CompanyDto> searchCompanies(SearchWithSerpRequest request) {
    return List.of();
  }

  @Override
  public AiClientResponse searchJobsFromCompanies(SearchWithSerpRequest request, List<CompanyDto> group) {
    return new AiClientResponse();
  }

  @SuppressWarnings("unused")
  private List<Job> fallbackSearch(@NotNull SearchWithSerpRequest request, Throwable t) {
    log.error("Serp search short-circuited/bulkheaded: {}", t.getMessage());
    return List.of();
  }

  private SerpJobsResult consolidate(SerpJobsResult results1, SerpJobsResult results2) {
    List<SerpJobHit> jobs = new ArrayList<>(results1.jobs());
    jobs.addAll(results2.jobs());
    return new SerpJobsResult(results1.id(), jobs, results2.nextPageToken());
  }

  private SerpJobsResult searchJobsPagination(SearchWithSerpRequest request, String nextPageToken) throws IOException {
    log.info("Searching jobs with Serp Api, query: {}", request.getQuery());
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

  private URI buildUri(SearchWithSerpRequest request, String nextPageToken) {
    List<String> parameters = new ArrayList<>();
    parameters.add("api_key");
    parameters.add(applicationProperties.getSerp().getApiKey());
    parameters.add("engine");
    parameters.add(request.getModel().getModel().toLowerCase());
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
      String value = switch (request.getWorkType()) {
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

  private String encodeLocation(@NotBlank String location) {
    byte[] utf8Bytes = location.getBytes(StandardCharsets.UTF_8);
    String base64 = Base64.getEncoder().encodeToString(utf8Bytes);
    return UULE_PREFIX + base64;
  }

  private double kilometersToMiles(double kilometers) {
    if (kilometers < 0) {
      throw new RuntimeException("Distance cannot be negative");
    }
    return kilometers * KM_TO_MILES;
  }

}
