package com.jobshunter.dto.gptRequest;

import com.jobshunter.dto.gptRequest.tools.Tools;
import java.util.List;

public record GptJobsPayload(
    String model,
    Reasoning reasoning,
    double temperature,
    int max_output_tokens,
    List<Tools> tools,
    String instructions,
    List<Input> input
) {

}
