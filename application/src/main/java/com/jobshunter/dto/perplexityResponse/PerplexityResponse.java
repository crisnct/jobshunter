package com.jobshunter.dto.perplexityResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Response of {@code POST /v1/responses}. {@code status} is one of completed | failed | incomplete | in_progress | queued | cancelled.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PerplexityResponse(
    String id,
    String status,
    String model,
    List<OutputItem> output,
    Usage usage,
    ResponseError error
) {

  public static final String STATUS_INCOMPLETE = "incomplete";
  public static final String STATUS_FAILED = "failed";
}
