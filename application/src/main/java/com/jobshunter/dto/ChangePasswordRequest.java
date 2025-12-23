package com.jobshunter.dto;

import com.jobshunter.processor.SqlInjectionSafe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "oldPassword must not be blank")
    @Size(max = 255)
    @SqlInjectionSafe
    String oldPassword,

    @NotBlank(message = "newPassword must not be blank")
    @Size(min = 8, message = "newPassword must be at least 8 characters")
    @Size(max = 255)
    @SqlInjectionSafe
    String newPassword
) {

}
