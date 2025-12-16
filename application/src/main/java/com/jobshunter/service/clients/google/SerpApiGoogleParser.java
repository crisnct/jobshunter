package com.jobshunter.service.clients.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class SerpApiGoogleParser {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  ParseResult parse(String rawJson) throws IOException {
    JsonNode root = MAPPER.readTree(rawJson);

    // 1) If SerpAPI returns an explicit error, handle it cleanly
    String error = textOrNull(root, "error");
    if (error != null) {
      return ParseResult.error(error, readJobsState(root), readFilters(root));
    }

    // 2) Some responses have "Fully empty" state and no jobs_results
    String jobsState = readJobsState(root);
    JsonNode jobsResultsNode = findJobsArray(root);

    if ("Fully empty".equalsIgnoreCase(jobsState) || jobsResultsNode == null || !jobsResultsNode.isArray()) {
      return ParseResult.empty(jobsState, readFilters(root));
    }

    // 3) Normal case: parse jobs_results[]
    List<JobHit> jobs = new ArrayList<>();
    for (JsonNode job : jobsResultsNode) {
      String title = job.path("title").asText("");
      String company = job.path("company_name").asText("");
      String location = job.path("location").asText("");
      String shareLink = job.path("share_link").asText("");
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

      jobs.add(new JobHit(title, company, location, shareLink, jobId, applyLinks));
    }

    return ParseResult.success(jobs, readFilters(root));
  }

  private String readJobsState(JsonNode root) {
    return root.path("search_information")
        .path("jobs_results_state")
        .asText(null);
  }

  /**
   * SerpAPI sometimes returns the jobs array as "jobs_results" (documented) but there are payloads seen in the wild using "job_results". Try both so
   * we do not silently drop results.
   */
  private JsonNode findJobsArray(JsonNode root) {
    JsonNode jobs = root.get("jobs_results");
    if (jobs == null || !jobs.isArray()) {
      jobs = root.get("job_results");
    }
    return jobs;
  }

  private List<FilterLink> readFilters(JsonNode root) {
    List<FilterLink> filters = new ArrayList<>();
    JsonNode filtersNode = root.path("filters");
    if (filtersNode.isArray()) {
      for (JsonNode f : filtersNode) {
        filters.add(new FilterLink(
            f.path("name").asText(""),
            f.path("link").asText(""),
            f.path("serpapi_link").asText("")
        ));
      }
    }
    return filters;
  }

  private String textOrNull(JsonNode node, String field) {
    JsonNode v = node.get(field);
    return (v == null || v.isNull()) ? null : v.asText();
  }

}
