package com.jobshunter.dto;

import com.jobshunter.processor.SqlInjectionSafe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserPromptRequest(
    long id,
    @NotBlank(message = "Prompt cannot be blank")
    @Size(max=3000)
    @SqlInjectionSafe
    String prompt,

    @NotNull
    EngineType engine
) {
}

