package com.jobshunter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IpInfoResponse(
    String country,
    @JsonProperty("country_code")
    String countryCode,
    @JsonProperty("continent_code")
    String continentCode,
    String asn,
    Boolean bogon
) {

}
