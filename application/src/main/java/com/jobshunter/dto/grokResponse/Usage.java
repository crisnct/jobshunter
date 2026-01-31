package com.jobshunter.dto.grokResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Usage(
    @JsonProperty("input_tokens") int inputTokens,
    @JsonProperty("input_tokens_details") InputTokensDetails inputTokensDetails,
    @JsonProperty("output_tokens") int outputTokens,
    @JsonProperty("output_tokens_details") OutputTokensDetails outputTokensDetails,
    @JsonProperty("total_tokens") int totalTokens,
    @JsonProperty("num_sources_used") Integer numSourcesUsed,
    @JsonProperty("num_server_side_tools_used") Integer numServerSideToolsUsed,
    @JsonProperty("cost_in_usd_ticks") Long costInUsdTicks
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record InputTokensDetails(@JsonProperty("cached_tokens") Integer cachedTokens) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record OutputTokensDetails(@JsonProperty("reasoning_tokens") Integer reasoningTokens) {
  }
}
