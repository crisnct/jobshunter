package com.jobshunter.dto.perplexityRequest.tools;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Server-side tools of the Perplexity Agent API, serialized with the {@code type} discriminator.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = WebSearchTool.class, name = "web_search"),
    @JsonSubTypes.Type(value = FetchUrlTool.class, name = "fetch_url")
})
public sealed interface Tool permits WebSearchTool, FetchUrlTool {

}
