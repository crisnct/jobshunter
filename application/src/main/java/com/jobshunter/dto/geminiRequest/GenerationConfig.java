package com.jobshunter.dto.geminiRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class GenerationConfig {

  private Double temperature;
  private Double topP;
  private Integer topK;

  @JsonProperty("max_output_tokens")
  private Integer maxOutputTokens;
  private Integer candidateCount;
  private Double presencePenalty;
  private Double frequencyPenalty;
  private List<String> stopSequences;
  @JsonProperty("response_mime_type")
  private String responseMimeType;

  @JsonProperty("response_schema")
  private Object responseJsonSchema;

  private ThinkingConfig thinkingConfig;

  public static class GenerationConfigBuilder {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().findAndAddModules().build();

    public GenerationConfigBuilder responseJsonSchema(String schema) {
      try {
        responseJsonSchema = JSON_MAPPER.readValue(schema, Map.class);
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid schema JSON", e);
      }
      return this;
    }
  }

}
