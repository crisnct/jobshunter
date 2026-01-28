package com.jobshunter.dto.gptResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OutputTokensDetails(

    @JsonProperty("reasoning_tokens")
    int reasoningTokens

) {
}
