package com.jobshunter.dto.perplexityRequest.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FetchUrlTool(
    @JsonProperty("max_urls")
    Integer maxUrls
) implements Tool {

}
