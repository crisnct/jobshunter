package com.jobshunter.service.clients.serpapi;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.config.ApplicationProperties.SerpApi;
import com.jobshunter.dto.SearchWithSerpRequest;
import com.jobshunter.dto.SerpApiJobHit;
import com.jobshunter.dto.SerpApiJobsResult;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Read documentation here
 * <a href="https://serpapi.com/google-jobs-api">https://serpapi.com/google-jobs-api</a>
 */
@Slf4j
@Component
public class SerpApiClient {

  private static final URI BASE = URI.create("https://serpapi.com/search");

  @Autowired
  private RestClient restClient;

  private final SerpApi serpApiConfig;

  public SerpApiClient(ApplicationProperties properties) {
    serpApiConfig = properties.getSerpApi();
  }

  public SerpApiJobsResult searchJobs(@NotNull SearchWithSerpRequest request) throws IOException {
    SerpApiJobsResult results = searchJobsPagination(request, null);
    for (int i = 0; i < serpApiConfig.getMaxPageSearch(); i++) {
      if (results.nextPageToken() == null) {
        break;
      } else {
        results = consolidate(results, searchJobsPagination(request, results.nextPageToken()));
      }
    }
    return results;
  }

  private SerpApiJobsResult consolidate(SerpApiJobsResult results1, SerpApiJobsResult results2) {
    List<SerpApiJobHit> jobs = new ArrayList<>(results1.jobs());
    jobs.addAll(results2.jobs());
    return new SerpApiJobsResult(jobs, results2.nextPageToken());
  }

  private SerpApiJobsResult searchJobsPagination(SearchWithSerpRequest request, String nextPageToken) throws IOException {
    log.info("Searching jobs with Serp Api, query: {}", request.query());
    URI uri = this.buildUri(request, nextPageToken);
    ResponseEntity<String> response = restClient.get()
        .uri(uri)
        .retrieve()
        .onStatus(HttpStatusCode::is2xxSuccessful, (req, res) -> {
          log.info("SERP API request executed successfully");
        })
        .onStatus(HttpStatusCode::isError, (req, res) -> {
          String error = new String(res.getBody().readAllBytes());
          throw new IllegalStateException("SERP API failed: " + res.getStatusCode() + " " + error);
        })
        .toEntity(String.class);

    return new SerpApiJobsResponseParser().parse(response.getBody());
  }

  private URI buildUri(SearchWithSerpRequest request, String nextPageToken) {
    List<String> parameters = new ArrayList<>();
    parameters.add("api_key");
    parameters.add(serpApiConfig.getApiKey());
    parameters.add("engine");
    parameters.add("google_jobs");
    parameters.add("q");
    parameters.add(request.query());
    parameters.add("chips");
    if (request.datePosted() == null) {
      parameters.add("date_posted:week");
    } else {
      parameters.add("date_posted:" + request.datePosted());
    }
    if (request.googleDomain() != null) {
      parameters.add("google_domain");
      parameters.add(request.googleDomain());
    }
    if (request.language() != null) {
      parameters.add("hl");
      parameters.add(request.language());
    }
    if (request.country() != null) {
      parameters.add("gl");
      parameters.add(request.country());
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
      sb.append(encode(kv[i])).append("=").append(encode(kv[i + 1]));
    }
    return URI.create(sb.toString());
  }

  private static String encode(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

}
