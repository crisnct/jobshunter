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

    @JsonProperty("main_careers_page_url")
    String careersPage

) {

  public CompanyDto {
    if (companyName == null || companyName.isBlank()) {
      throw new IllegalArgumentException("company_name must not be null or blank");
    }
    if (careersPage == null || careersPage.isBlank()) {
      throw new IllegalArgumentException("main_careers_page_url must not be null or blank");
    }
  }
}
