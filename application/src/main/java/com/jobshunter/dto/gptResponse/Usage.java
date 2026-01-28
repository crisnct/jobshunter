package com.jobshunter.dto.gptResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Usage(

    @JsonProperty("input_tokens")
    int inputTokens,

    @JsonProperty("input_tokens_details")
    InputTokensDetails inputTokensDetails,

    @JsonProperty("output_tokens")
    int outputTokens,

    @JsonProperty("output_tokens_details")
    OutputTokensDetails outputTokensDetails,

    @JsonProperty("total_tokens")
    int totalTokens

) {
}
