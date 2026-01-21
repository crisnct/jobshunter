package com.jobshunter.dto.serpResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Country(
    @JsonProperty("country_code")
    String countryCode,
    @JsonProperty("country_name")
    String countryName
) {
}