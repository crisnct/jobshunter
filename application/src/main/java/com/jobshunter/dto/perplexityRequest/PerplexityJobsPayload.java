package com.jobshunter.dto.perplexityRequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.dto.perplexityRequest.tools.Tool;
import com.jobshunter.model.AiCapabilityType;
import com.jobshunter.service.AiCapabilityChecker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Request body of the Perplexity Agent API ({@code POST /v1/responses}, OpenAI Responses format).
 * <p>
 * Differences from the Grok/GPT payloads: the structured output is {@code response_format} (not {@code text.format}), the agent loop is bounded
 * explicitly with {@code max_steps}, and the web search tool carries its own filters.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PerplexityJobsPayload(
    String model,
    Double temperature,
    @JsonProperty("max_output_tokens")
    Integer maxOutputTokens,
    @JsonProperty("max_steps")
    Integer maxSteps,
    Reasoning reasoning,
    @JsonProperty("previous_response_id")
    String previousResponseId,
    List<Tool> tools,
    String instructions,
    @JsonProperty("response_format")
    ResponseFormat responseFormat,
    Boolean store,
    List<Object> input,
    @JsonIgnore
    AiModelEntity aiModel
) {

  public PerplexityJobsPayload {
    if (store == null) {
      store = Boolean.FALSE;
    }
  }

  public static Builder builder(AiModelEntity model) {
    return new Builder(model);
  }

  /**
   * Builder that silently skips every feature the model does not declare in {@code ai_models_capability}, like the other providers.
   */
  public static final class Builder {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private final AiModelEntity aiModel;
    private final List<Object> input = new ArrayList<>();
    private final List<Tool> tools = new ArrayList<>();

    private Double temperature;
    private Integer maxOutputTokens;
    private Integer maxSteps;
    private Reasoning reasoning;
    private String previousResponseId;
    private String instructions;
    private ResponseFormat responseFormat;
    private Boolean store;

    private Builder(AiModelEntity model) {
      this.aiModel = model;
    }

    public Builder temperature(Double temperature) {
      if (AiCapabilityChecker.isEnabled(aiModel, AiCapabilityType.TEMPERATURE)) {
        this.temperature = temperature;
      }
      return this;
    }

    public Builder maxOutputTokens(Integer maxOutputTokens) {
      this.maxOutputTokens = maxOutputTokens;
      return this;
    }

    public Builder maxSteps(Integer maxSteps) {
      this.maxSteps = maxSteps;
      return this;
    }

    public Builder instructions(String instructions) {
      if (AiCapabilityChecker.isEnabled(aiModel, AiCapabilityType.SYSTEM_PROMPT)) {
        this.instructions = instructions;
      }
      return this;
    }

    public Builder reasoning(Reasoning reasoning) {
      if (AiCapabilityChecker.isEnabled(aiModel, AiCapabilityType.REASONING)) {
        this.reasoning = reasoning;
      }
      return this;
    }

    public Builder store(Boolean store) {
      this.store = store;
      return this;
    }

    public Builder previousResponseId(String previousResponseId) {
      this.previousResponseId = previousResponseId;
      return this;
    }

    public Builder addSystemPrompt(String systemPrompt) {
      if (AiCapabilityChecker.isEnabled(aiModel, AiCapabilityType.SYSTEM_PROMPT)) {
        input.add(new Input("system", List.of(new InputMessage("input_text", systemPrompt))));
      }
      return this;
    }

    public Builder addUserPrompt(String userPrompt) {
      input.add(new Input("user", List.of(new InputMessage("input_text", userPrompt))));
      return this;
    }

    public Builder setResponseSchema(String name, String schema) {
      if (AiCapabilityChecker.isEnabled(aiModel, AiCapabilityType.RESPONSE_SCHEMA)) {
        try {
          this.responseFormat = ResponseFormat.jsonSchema(name, JSON_MAPPER.readValue(schema, Map.class));
        } catch (Exception e) {
          throw new IllegalArgumentException("Invalid schema JSON", e);
        }
      }
      return this;
    }

    /** Adds a server-side tool (web_search / fetch_url), only when the model supports browsing. */
    public Builder addTool(Tool tool) {
      if (AiCapabilityChecker.isEnabled(aiModel, AiCapabilityType.WEB_SEARCH)) {
        this.tools.add(tool);
      }
      return this;
    }

    public PerplexityJobsPayload build() {
      if (reasoning != null && temperature != null) {
        throw new IllegalStateException("TEMPERATURE and REASONING can not be set both for model " + aiModel.getModel());
      }
      return new PerplexityJobsPayload(
          aiModel.getModel(),
          temperature,
          maxOutputTokens,
          maxSteps,
          reasoning,
          previousResponseId,
          tools.isEmpty() ? null : List.copyOf(tools),
          instructions,
          responseFormat,
          store,
          List.copyOf(input),
          aiModel
      );
    }
  }
}
