package com.jobshunter.dto.gptRequest;

//@formatter:off
/**
 * Represents a single job listing returned by the GPT response.
 */
 //@formatter:on
public record JobResult(
    String job_url
) {
}
