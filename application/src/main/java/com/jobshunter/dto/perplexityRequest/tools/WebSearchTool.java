package com.jobshunter.dto.perplexityRequest.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebSearchTool(
    @JsonProperty("search_context_size")
    String searchContextSize,
    @JsonProperty("max_results")
    Integer maxResults,
    @JsonProperty("user_location")
    UserLocation userLocation,
    WebSearchFilters filters
) implements Tool {

}
