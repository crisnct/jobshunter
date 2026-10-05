package com.jobshunter.dto.perplexityResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolCallDetail(
    Integer invocation,
    @JsonProperty("cost_usd") Double costUsd
) {

}
