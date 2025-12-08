package com.jobshunter.dto;


import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class EmailRequest {
    @NotBlank private String email;
    @Nullable private String subject;
    @Nullable private String message;
    @Nullable private MultipartFile file;
}
