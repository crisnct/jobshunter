package com.jobshunter.dto.gptResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FileListResponse(
    String object,
    List<FileInfo> data,
    @JsonProperty("has_more") boolean hasMore,
    @JsonProperty("first_id") String firstId,
    @JsonProperty("last_id") String lastId
) {
}

