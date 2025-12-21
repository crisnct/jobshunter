package com.jobshunter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "oldPassword must not be blank")
    @Size(max = 255)
    String oldPassword,

    @NotBlank(message = "newPassword must not be blank")
    @Size(min = 8, message = "newPassword must be at least 8 characters")
    @Size(max = 255)
    String newPassword
) {

}
