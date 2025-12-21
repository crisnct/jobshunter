package com.jobshunter.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record SearchJobsRequest(
    @NotBlank(message = "username is required")
    @Size(max = 255)
    String username,

    @Positive(message = "iterations must be positive")
    @Max(10)
    int iterations,

    @NotEmpty(message = "engines must not be empty")
    @Size(max = 20)
    Set<EngineType> engines
) {
}
