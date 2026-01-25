package com.jobshunter.dto.gptResponse;

//@formatter:off
/**
 * Represents a single job listing returned by the GPT response.
 */
 //@formatter:on
public record JobResult(
    String job_posting_url,
    String company_name
) {

}
