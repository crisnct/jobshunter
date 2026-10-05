package com.jobshunter.dto.perplexityResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResponseError(String type, String code, String message) {

}
