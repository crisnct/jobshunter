package com.jobshunter.dto.gptRequest;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TextFormat(
    String name,
    String type,
    Object schema
) {
}
