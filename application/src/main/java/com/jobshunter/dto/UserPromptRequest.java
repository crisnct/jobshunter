package com.jobshunter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPromptRequest(
    @NotBlank(message = "Prompt cannot be blank") String prompt,
    @NotBlank(message = "Engine cannot be blank") @Size(max = 255, message = "Engine must be at most 255 characters") String engine
) {
}

