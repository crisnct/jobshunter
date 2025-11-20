package com.jobshunter.dto;

import jakarta.validation.constraints.NotBlank;

public record JobSearchRequest(
        @NotBlank(message = "Prompt cannot be blank") String prompt,
        @NotBlank(message = "Path to the CV PDF is required") String cvPath) {
}
