package com.jobshunter.dto.perplexityRequest.tools;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Perplexity user location. Unlike Grok there is no {@code type: "approximate"} discriminator.
 *
 * @param country ISO 3166-1 alpha-2 code, e.g. RO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserLocation(String country, String city, String region) {

}
