package com.jobshunter.dto.geminiResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FileListResponse(
    List<FileInfo> files,
    String nextPageToken
) {

}

