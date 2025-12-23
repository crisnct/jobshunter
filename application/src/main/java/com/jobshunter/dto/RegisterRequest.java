package com.jobshunter.dto;

import com.jobshunter.processor.SqlInjectionSafe;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank
    @Size(min = 3, max = 50)
    @SqlInjectionSafe
    String username,

    @NotBlank
    @Email
    @SqlInjectionSafe
    String email,

    @NotBlank
    @Size(min = 8, max = 255)
    @SqlInjectionSafe
    String password,

    @NotBlank
    @Size(max = 30)
    @SqlInjectionSafe
    String phoneNumber
) {

}
