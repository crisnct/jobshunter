package com.jobshunter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jobshunter.processor.SqlInjectionSafe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchCompaniesRequest(
    @NotBlank
    @SqlInjectionSafe
    String engineType,

    @NotBlank
    @SqlInjectionSafe
    String aiModel,

    @NotBlank
    @SqlInjectionSafe
    String city,

    @SqlInjectionSafe
    String country,

    @SqlInjectionSafe
    String domain,

    @NotNull
    @NotEmpty
    List<@NotBlank String> positions
) {

}
