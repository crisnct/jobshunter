package com.jobshunter.dto.perplexityResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Structured content of the assistant message, as dictated by {@code perplexity_json_schema_response.json}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JobSearchResponse(
    List<JobResult> results
) {

}
