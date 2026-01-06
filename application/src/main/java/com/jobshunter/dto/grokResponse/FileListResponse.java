package com.jobshunter.dto.grokResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FileListResponse(
    List<FileInfo> data
) {

}

