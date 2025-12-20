package com.jobshunter.dto.gptRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jobshunter.dto.gptRequest.tools.Tools;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GptJobsPayload(
    String model,
    Reasoning reasoning,
    int max_output_tokens,
    List<Tools> tools,
    String instructions,
    Text text,
    List<Input> input
) {

}
