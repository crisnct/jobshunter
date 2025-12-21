package com.jobshunter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank
    @Size(max = 255)
    String username,

    @Size(max = 255)
    @NotBlank
    String password
) {
}
