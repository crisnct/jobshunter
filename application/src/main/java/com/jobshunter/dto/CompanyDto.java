package com.jobshunter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CompanyDto(
    @JsonProperty("company_name")
    String companyName,
    @JsonProperty("company_location")
    String companyLocation
) {

}