package com.jobshunter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jobshunter.processor.SqlInjectionSafe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record JobScoreRequestDto(
    @NotBlank
    @Size(max = 2048)
    @SqlInjectionSafe
    String jobUrl,
    
    @NotBlank
    @SqlInjectionSafe
    String engineProvider,
    
    @NotBlank
    @SqlInjectionSafe
    String engineModel
) {
}
