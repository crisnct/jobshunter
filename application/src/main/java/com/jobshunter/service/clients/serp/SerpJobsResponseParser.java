package com.jobshunter.service.clients.serp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobshunter.dto.serpResponse.SerpJobHit;
import com.jobshunter.dto.serpResponse.SerpJobsResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class SerpJobsResponseParser {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  SerpJobsResult parse(String rawJson) throws IOException {
    JsonNode root = MAPPER.readTree(rawJson);

    // 1) If Serp returns an explicit error, handle it cleanly
    String error = textOrNull(root, "error");
    if (error != null) {
      return SerpJobsResult.empty();
    }

    // 2) Some responses have "Fully empty" state and no jobs_results
    JsonNode jobsResultsNode = findJobsArray(root);

    // 3) Normal case: parse jobs_results[]
    List<SerpJobHit> jobs = new ArrayList<>();
    for (JsonNode job : jobsResultsNode) {
      String title = job.path("title").asText("");
      String company = job.path("company_name").asText("");
      String location = job.path("location").asText("");
      String description = job.path("description").asText("");
      String highlights = job.path("job_highlights").toPrettyString();
      String jobId = job.path("job_id").asText("");

      List<String> applyLinks = new ArrayList<>();
      JsonNode applyOptions = job.path("apply_options");
      if (applyOptions.isArray()) {
        for (JsonNode opt : applyOptions) {
          String link = opt.path("link").asText("");
          if (!link.isBlank()) {
            applyLinks.add(link);
          }
        }
      }

      jobs.add(new SerpJobHit(title, company, location, description, highlights, jobId, applyLinks));
    }

    return SerpJobsResult.success(jobs, readNextPageToken(root));
  }

  private String readNextPageToken(JsonNode root) {
    String token = null;
    JsonNode page = root.get("serpapi_pagination");
    if (page != null) {
      JsonNode nextPageToken = page.get("next_page_token");
      if (nextPageToken != null) {
        token = nextPageToken.asText();
      }
    }
    return token;
  }

  private String readJobsState(JsonNode root) {
    return root.path("search_information")
        .path("jobs_results_state")
        .asText(null);
  }

  /**
   * Serp sometimes returns the jobs array as "jobs_results" (documented) but there are payloads seen in the wild using "job_results". Try both so
   * we do not silently drop results.
   */
  private JsonNode findJobsArray(JsonNode root) {
    JsonNode jobs = root.get("jobs_results");
    if (jobs == null || !jobs.isArray()) {
      jobs = root.get("job_results");
    }
    return jobs;
  }

  private String textOrNull(JsonNode node, String field) {
    JsonNode v = node.get(field);
    return (v == null || v.isNull()) ? null : v.asText();
  }

}
