package com.jobshunter.dto.gptRequest;

import java.util.List;

public record Gpt4ScorePayload(
    String model,
    double temperature,
    int max_output_tokens,
    List<Input> input
) {

}