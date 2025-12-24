package com.jobshunter.dto.gptResponse;

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
    @JsonProperty("expires_at") Long expiresAt,
    String status,
    @JsonProperty("status_details") String statusDetails
) {
}

