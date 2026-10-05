package com.jobshunter.dto.perplexityRequest.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * @param searchDomainFilter  max 20 entries; either an allowlist or a denylist (entries prefixed with {@code -}), never mixed
 * @param searchRecencyFilter hour | day | week | month | year
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebSearchFilters(
    @JsonProperty("search_domain_filter")
    List<String> searchDomainFilter,
    @JsonProperty("search_recency_filter")
    String searchRecencyFilter
) {

  /** Max number of entries accepted by the API in {@code search_domain_filter}. */
  public static final int MAX_DOMAINS = 20;
}
