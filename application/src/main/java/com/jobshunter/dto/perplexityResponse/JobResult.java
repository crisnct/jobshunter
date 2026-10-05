package com.jobshunter.dto.perplexityResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobResult(
    String job_posting_url,
    String company_name
) {

}
