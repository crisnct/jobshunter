package com.jobshunter.dto.grokResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GrokResponse(UUID id, List<OutputItem> output) {

}
