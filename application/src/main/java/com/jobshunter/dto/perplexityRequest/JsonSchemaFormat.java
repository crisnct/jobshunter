package com.jobshunter.dto.perplexityRequest;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonSchemaFormat(
    String name,
    Object schema,
    Boolean strict
) {

}
