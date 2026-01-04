package com.jobshunter.dto.geminiResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FileInfo(

    String name,
    String displayName,
    String mimeType,

    @JsonProperty("sizeBytes")
    Long sizeBytes,   // STRING în JSON → Jackson convertește corect

    Instant createTime,
    Instant updateTime,
    Instant expirationTime,

    String sha256Hash,
    String uri,
    String state,
    String source
) {

}
