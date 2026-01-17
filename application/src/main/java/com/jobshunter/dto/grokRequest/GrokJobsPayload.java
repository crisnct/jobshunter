package com.jobshunter.dto.grokRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.dto.grokRequest.tools.Tools;
import com.jobshunter.model.AiCapabilityType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record GrokJobsPayload(
    String model,
    Double temperature,
    @JsonProperty("max_output_tokens")
    int maxOutputTokens,
    Reasoning reasoning,
    @JsonProperty("previous_response_id")
    String previousResponseId,
    List<Tools> tools,
    String instructions,
    Text text,
    Boolean store,
    List<Object> input
) {

  public GrokJobsPayload {
    if (store == null) {
      store = Boolean.FALSE;
    }
  }

  public static GrokJobsPayloadBuilder builder(AiModelEntity model) {
    return new GrokJobsPayloadBuilder(model);
  }

  @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "FieldMayBeFinal", "FieldCanBeLocal", "unused"})
  @Slf4j
  public static class GrokJobsPayloadBuilder {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private List<Object> input = new ArrayList<>();

    private Text text;

    private List<Tools> tools = new ArrayList<>();

    private AiModelEntity aiModel;

    private GrokJobsPayloadBuilder(AiModelEntity model) {
      this.aiModel = model;
      this.model = model.getModel();
    }

    public GrokJobsPayloadBuilder temperature(Double temperature) {
      if (isEnabledCapability(AiCapabilityType.TEMPERATURE)) {
        this.temperature = temperature;
      }
      return this;
    }

    public GrokJobsPayloadBuilder instructions(String instructions) {
      if (isEnabledCapability(AiCapabilityType.SYSTEM_PROMPT)) {
        this.instructions = instructions;
      }
      return this;
    }

    public GrokJobsPayloadBuilder reasoning(Reasoning reasoning) {
      if (isEnabledCapability(AiCapabilityType.REASONING)) {
        this.reasoning = reasoning;
      }
      return this;
    }

    public GrokJobsPayloadBuilder store(Boolean store) {
      if (!store || isEnabledCapability(AiCapabilityType.CHAIN_CONVERSATIONS)) {
        this.store = store;
      }
      return this;
    }

    public GrokJobsPayloadBuilder previousResponseId(String previousResponseId) {
      if (isEnabledCapability(AiCapabilityType.CHAIN_CONVERSATIONS)) {
        this.previousResponseId = previousResponseId;
      }
      return this;
    }

    public GrokJobsPayloadBuilder addSystemPrompt(String systemPrompt) {
      if (isEnabledCapability(AiCapabilityType.SYSTEM_PROMPT)) {
        input.add(new Input("system", List.of(new InputMessage("input_text", systemPrompt))));
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

    public GrokJobsPayloadBuilder addUserPrompt(String userPrompt, String fileId) {
      List<InputObj> inputs = new ArrayList<>();
      inputs.add(new InputMessage("input_text", userPrompt));
      if (Strings.isNotBlank(fileId) && isEnabledCapability(AiCapabilityType.FILE_UPLOAD)) {
        inputs.add(new InputFile(fileId));
      }
      input.add(new Input("user", inputs));
      return this;
    }

    public GrokJobsPayloadBuilder addUserPrompt(String userPrompt) {
      input.add(new Input("user", List.of(
          new InputMessage("input_text", userPrompt)
      )));
      return this;
    }

    public GrokJobsPayloadBuilder addAssistantPrompt(String prompt) {
      if (isEnabledCapability(AiCapabilityType.CHAIN_CONVERSATIONS)) {
        input.add(new AssistantInput("assistant", prompt));
      }
      return this;
    }

    public GrokJobsPayloadBuilder setResponseSchema(String schema) {
      if (isEnabledCapability(AiCapabilityType.RESPONSE_SCHEMA)) {
        try {
          this.text = new Text(
              new TextFormat(
                  "job_search_results",
                  "json_schema",
                  JSON_MAPPER.readValue(schema, Map.class)
              )
          );
        } catch (Exception e) {
          throw new IllegalArgumentException("Invalid schema JSON", e);
        }
      }
      return this;
    }

    public GrokJobsPayloadBuilder addTools(Tools tool) {
      if (isEnabledCapability(AiCapabilityType.WEB_SEARCH)) {
        this.tools.add(tool);
      }
      return this;
    }

  }
}
