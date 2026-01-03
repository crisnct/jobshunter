package com.jobshunter.dto.grokRequest;

import java.util.List;

//@formatter:off
/**
 * Root DTO for GPT job search responses.
 */
 //@formatter:on
public record JobSearchResponse(
    List<JobResult> results
) {
}
