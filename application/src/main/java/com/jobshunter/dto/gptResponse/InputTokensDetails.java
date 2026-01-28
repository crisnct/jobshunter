package com.jobshunter.dto.gptResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InputTokensDetails(

    @JsonProperty("cached_tokens")
    int cachedTokens

) {

}
