package com.jobshunter.dto;

import com.jobshunter.processor.SqlInjectionSafe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank
    @Size(max = 255)
    @SqlInjectionSafe
    String username,

    @Size(max = 255)
    @NotBlank
    @SqlInjectionSafe
    String password
) {
}
