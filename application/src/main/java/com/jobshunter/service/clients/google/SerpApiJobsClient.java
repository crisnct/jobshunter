package com.jobshunter.service.clients.google;

import com.jobshunter.controller.TestController.SearchWithSerpRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class SerpApiJobsClient {

  private static final URI BASE = URI.create("https://serpapi.com/search");

  @Autowired
  private RestClient restClient;

  @Value("${jobshunter.google.serpApiKey:}")
  private String apiKey;

  public ParseResult searchJobs(SearchWithSerpRequest request) throws IOException {
    Objects.requireNonNull(request);
    Objects.requireNonNull(request.query());
    Objects.requireNonNull(request.engine());

    URI uri = this.buildUri(request);
    ResponseEntity<String> response = restClient.get()
        .uri(uri)
        .retrieve()
        .onStatus(HttpStatusCode::isError, (req, res) -> {
          res.getBody();
          String error = new String(res.getBody().readAllBytes());
          throw new IllegalStateException("SERP API failed: " + res.getStatusCode() + " " + error);
        })
        .toEntity(String.class);

    if (response.getStatusCode() != HttpStatusCode.valueOf(200)) {
      throw new IOException("SerpAPI returned HTTP " + response.getStatusCode() + ": " + response.getBody());
    }

    return new SerpApiGoogleParser().parse(response.getBody());
  }

  private URI buildUri(SearchWithSerpRequest request) {
    List<String> parameters = new ArrayList<>();
    parameters.add("engine");
    //linkedin_jobs, google_jobs, google_jobs_listing, google
    parameters.add(request.engine());
    parameters.add("q");
    parameters.add(request.query());
    if (request.language() != null) {
      parameters.add("hl");
      parameters.add(request.language());
    }
    if (request.country() != null) {
      parameters.add("gl");
      parameters.add(request.country());
    }
    parameters.add("chips");
    parameters.add("date_posted:week");
    parameters.add("api_key");
    parameters.add(apiKey);

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
