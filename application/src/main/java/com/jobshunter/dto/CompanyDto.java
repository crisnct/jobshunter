package com.jobshunter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

//@formatter:off
/**
 * DTO representing a company that passed all binary evaluation criteria (score = 1).
 * Deterministic and audit-friendly.
 */
//@formatter:on
public record CompanyDto(

    @JsonProperty("company_name")
    String companyName,

    @JsonProperty("official_website_url")
    String officialWebsiteUrl

) {

  public CompanyDto {
    if (companyName == null || companyName.isBlank()) {
      throw new IllegalArgumentException("companyName must not be null or blank");
    }
    if (officialWebsiteUrl == null || officialWebsiteUrl.isBlank()) {
      throw new IllegalArgumentException("officialWebsiteUrl must not be null or blank");
    }
  }
}
