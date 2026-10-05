package com.jobshunter.dto.perplexityResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Exact cost of the request in USD, computed by Perplexity (tokens, tools and cache included).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Cost(
    String currency,
    @JsonProperty("input_cost") Double inputCost,
    @JsonProperty("output_cost") Double outputCost,
    @JsonProperty("tool_calls_cost") Double toolCallsCost,
    @JsonProperty("total_cost") Double totalCost
) {

}
