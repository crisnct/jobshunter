package com.jobshunter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IpInfoDetailResponse(
    String region,
    String country,
    String org,
    String city,
    String timezone,
    Boolean bogon
) {

}
