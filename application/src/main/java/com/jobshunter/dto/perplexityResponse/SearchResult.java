package com.jobshunter.dto.perplexityResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One hit of a server-side {@code web_search}; also reused for the pages brought by {@code fetch_url} (which only fill url/title/snippet).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResult(
    String url,
    String title,
    String snippet,
    String date,
    @JsonProperty("last_updated") String lastUpdated
) {

}
