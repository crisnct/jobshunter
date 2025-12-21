package com.jobshunter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserPromptRequest(
    long id,
    @NotBlank(message = "Prompt cannot be blank")
    @Size(max=3000)
    String prompt,

    @NotNull
    EngineType engine
) {
}

