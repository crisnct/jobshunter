package com.jobshunter.dto.grokResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FileInfo(
    String object,
    String id,
    String purpose,
    String filename,
    long bytes,
    @JsonProperty("created_at") long createdAt,
    @JsonProperty("expires_at") Long expiresAt
) {

}

