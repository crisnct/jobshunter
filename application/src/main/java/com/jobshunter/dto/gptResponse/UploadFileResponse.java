package com.jobshunter.dto.gptResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UploadFileResponse(String id, String filename, Instant expires_at) {

}