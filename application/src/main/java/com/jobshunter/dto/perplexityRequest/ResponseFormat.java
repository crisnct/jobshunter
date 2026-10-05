package com.jobshunter.dto.perplexityRequest;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Structured output of the Perplexity Agent API. Unlike GPT/Grok ({@code text.format}) it lives at the top level of the payload as
 * {@code response_format}.
 */
public record ResponseFormat(
    String type,
    @JsonProperty("json_schema")
    JsonSchemaFormat jsonSchema
) {

  public static ResponseFormat jsonSchema(String name, Object schema) {
    return new ResponseFormat("json_schema", new JsonSchemaFormat(name, schema, true));
  }
}
