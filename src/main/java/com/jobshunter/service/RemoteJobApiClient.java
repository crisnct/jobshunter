package com.jobshunter.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobshunter.model.JobOpportunity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RemoteJobApiClient {

    private static final Logger log = LoggerFactory.getLogger(RemoteJobApiClient.class);
    private static final URI BASE_URI = URI.create("https://remotive.com/api/remote-jobs");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<JobOpportunity> search(String prompt) {
        try {
            String encoded = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
            URI uri = URI.create(BASE_URI + "?search=" + encoded);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("Job API returned {} - {}", response.statusCode(), response.body());
                return List.of();
            }
            RemotiveResponse body = objectMapper.readValue(response.body(), RemotiveResponse.class);
            return body.jobs == null ? List.of() : body.jobs.stream()
                    .map(RemoteJobApiClient::toOpportunity)
                    .collect(Collectors.toList());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Job API request failed: {}", e.getMessage());
            return List.of();
        }
    }

    private static JobOpportunity toOpportunity(RemotiveJob job) {
        return new JobOpportunity(
                job.title,
                job.companyName,
                job.candidateRequiredLocation,
                URI.create(job.url),
                job.publicationDate,
                job.tags == null ? List.of() : new ArrayList<>(job.tags),
                job.description
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemotiveResponse(List<RemotiveJob> jobs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemotiveJob(String title,
                               String companyName,
                               String candidateRequiredLocation,
                               String url,
                               OffsetDateTime publicationDate,
                               List<String> tags,
                               String description) {
    }
}
