package com.jobshunter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IpInfoDetailResponse(
    String region,
    String country,
    String org,
    String city,
    String loc,
    String timezone,
    Boolean bogon
) {

}
