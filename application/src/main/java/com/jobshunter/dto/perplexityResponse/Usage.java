package com.jobshunter.dto.perplexityResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Usage(
    @JsonProperty("input_tokens") int inputTokens,
    @JsonProperty("output_tokens") int outputTokens,
    @JsonProperty("total_tokens") int totalTokens,
    @JsonProperty("tool_calls_details") Map<String, ToolCallDetail> toolCallsDetails,
    Cost cost
) {

  /** Total number of server-side tool invocations (web_search, fetch_url, ...). */
  public int toolInvocations() {
    if (toolCallsDetails == null) {
      return 0;
    }
    return toolCallsDetails.values().stream()
        .filter(d -> d != null && d.invocation() != null)
        .mapToInt(ToolCallDetail::invocation)
        .sum();
  }
}
