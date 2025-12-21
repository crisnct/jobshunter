package com.jobshunter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Job(
    @Max(100)
    int score,

    @Size(max = 255)
    @NotNull
    String url,

    @Size(max = 255)
    String source
) {

}