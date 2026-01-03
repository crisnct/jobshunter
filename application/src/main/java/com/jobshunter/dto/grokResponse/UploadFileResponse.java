package com.jobshunter.dto.grokResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UploadFileResponse(String id) {

}