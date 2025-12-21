package com.jobshunter.dto;


import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class EmailRequest {
    @NotBlank
    private String email;

    @Nullable
    @Size(max = 255)
    private String subject;

    @NotBlank
    @Size(max = 10000)
    private String message;

    @Nullable
    private MultipartFile file;
}
