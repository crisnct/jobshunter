package com.jobshunter.dto.geminiRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.model.AiCapabilityType;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

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

  public static GenerationConfigBuilder builder(AiModelEntity model) {
    return new GenerationConfigBuilder(model);
  }

  @Slf4j
  public static class GenerationConfigBuilder {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private AiModelEntity aiModel;

    public GenerationConfigBuilder(AiModelEntity model) {
      this.aiModel = model;
    }

    public GenerationConfigBuilder thinkingConfig(ThinkingConfig config) {
      if (isEnabledCapability(AiCapabilityType.REASONING)) {
        thinkingConfig = config;
      }
      return this;
    }

    public GenerationConfigBuilder temperature(Double temperature) {
      if (isEnabledCapability(AiCapabilityType.TEMPERATURE)) {
        this.temperature = temperature;
      }
      return this;
    }

    public GenerationConfigBuilder responseJsonSchema(String schema) {
      if (isEnabledCapability(AiCapabilityType.RESPONSE_SCHEMA)) {
        try {
          responseJsonSchema = JSON_MAPPER.readValue(schema, Map.class);
        } catch (Exception e) {
          throw new IllegalArgumentException("Invalid schema JSON", e);
        }
      }
      return this;
    }

    private boolean isEnabledCapability(AiCapabilityType type) {
      boolean enabledCapability = aiModel.isEnabledCapability(type);
      if (!enabledCapability) {
        log.debug(type + " capability is not supported by model " + aiModel.getModel());
      }
      return enabledCapability;
    }

  }

}
