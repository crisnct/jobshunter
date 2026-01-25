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
public record SearchJobsByCompanyRequest(
    @NotNull
    @NotEmpty
    List<@NotBlank String> positions,

    @NotBlank
    @SqlInjectionSafe
    String company,

    @NotBlank
    @SqlInjectionSafe
    String company_url,

    @NotBlank
    @SqlInjectionSafe
    String engineType,

    @NotBlank
    @SqlInjectionSafe
    String aiModel,

    @NotBlank
    @SqlInjectionSafe
    String city,

    @NotBlank
    @SqlInjectionSafe
    String country,

    @NotBlank
    @SqlInjectionSafe
    String jobDomain
) {

}
