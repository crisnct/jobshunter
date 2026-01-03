package com.jobshunter.dto.gptResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GptResponse(List<OutputItem> output) {

}
