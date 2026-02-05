package com.jobshunter.dto.gptRequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.dto.exceptions.BusinessException;
import com.jobshunter.dto.gptRequest.tools.Tools;
import com.jobshunter.model.AiCapabilityType;
import com.jobshunter.service.AiCapabilityChecker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record GptJobsPayload(
    String model,
    Double temperature,
    Reasoning reasoning,
    @JsonProperty("max_output_tokens")
    Integer maxOutputTokens,
    List<Tools> tools,
    String instructions,
    Boolean store,
    @JsonProperty("previous_response_id")
    String previousResponseId,
    Text text,
    List<Object> input,
    @JsonIgnore
    AiModelEntity aiModel
) {

  public GptJobsPayload {
    if (store == null) {
      store = Boolean.FALSE;
    }
  }

  public static GptJobsPayloadBuilder builder(AiModelEntity model) {
    return new GptJobsPayloadBuilder(model);
  }

  @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "FieldMayBeFinal", "FieldCanBeLocal", "unused"})
  @Slf4j
  public static class GptJobsPayloadBuilder {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private List<Object> input = new ArrayList<>();

    private Text text;

    private List<Tools> tools = new ArrayList<>();

    private AiModelEntity aiModel;

    private GptJobsPayloadBuilder(AiModelEntity model) {
      this.aiModel = model;
      this.model = model.getModel();
    }

    public GptJobsPayloadBuilder temperature(Double temperature) {
      if (AiCapabilityChecker.isEnabled(aiModel, AiCapabilityType.TEMPERATURE)) {
        this.temperature = temperature;
      }
      return this;
    }

    public GptJobsPayloadBuilder store(Boolean store) {
      this.store = store;
      return this;
    }

    public GptJobsPayloadBuilder reasoning(Reasoning reasoning) {
      if (AiCapabilityChecker.isEnabled(aiModel, AiCapabilityType.REASONING)) {
        this.reasoning = reasoning;
      }
      return this;
    }

    public GptJobsPayloadBuilder addSystemPrompt(String systemPrompt) {
      if (AiCapabilityChecker.isEnabled(aiModel, AiCapabilityType.SYSTEM_PROMPT)) {
        input.add(new Input("system", List.of(new InputMessage("input_text", systemPrompt))));
      }
      return this;
    }

    public GptJobsPayloadBuilder addUserPrompt(String userPrompt, String fileId) {
      List<InputObj> inputs = new ArrayList<>();
      inputs.add(new InputMessage("input_text", userPrompt));
      if (Strings.isNotBlank(fileId) && AiCapabilityChecker.isEnabled(aiModel, AiCapabilityType.FILE_UPLOAD)) {
        inputs.add(new InputFile(fileId));
      }
      input.add(new Input("user", inputs));
      return this;
    }

    public GptJobsPayloadBuilder addUserPrompt(String userPrompt) {
      input.add(new Input("user", List.of(
          new InputMessage("input_text", userPrompt)
      )));
      return this;
    }

    public GptJobsPayloadBuilder addAssistantPrompt(String prompt) {
      input.add(new AssistantInput("assistant", prompt));
      return this;
    }

    public GptJobsPayloadBuilder addDeveloperPrompt(String userPrompt) {
      input.add(new Input("developer", List.of(
          new InputMessage("input_text", userPrompt)
      )));
      return this;
    }

    public GptJobsPayloadBuilder setResponseSchema(String schema) {
      if (AiCapabilityChecker.isEnabled(aiModel, AiCapabilityType.RESPONSE_SCHEMA)) {
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

    public GptJobsPayloadBuilder addTools(Tools tool) {
      if (AiCapabilityChecker.isEnabled(aiModel, AiCapabilityType.WEB_SEARCH)) {
        this.tools.add(tool);
      }
      return this;
    }

    public GptJobsPayload build() {
      if (reasoning != null && temperature != null) {
        throw new BusinessException(HttpStatus.NOT_FOUND, "TEMPERATURE and REASONING can not be set both for GPT models.");
      }
      return new GptJobsPayload(
          model,
          temperature,
          reasoning,
          maxOutputTokens,
          tools.isEmpty() ? null : tools,
          instructions,
          store,
          previousResponseId,
          text,
          input,
          aiModel
      );
    }

  }
}
