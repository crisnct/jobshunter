package com.jobshunter.dto.gptRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.dto.gptRequest.tools.Tools;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record GptJobsPayload(
    String model,
    Reasoning reasoning,
    @JsonProperty("max_output_tokens")
    int maxOutputTokens,
    List<Tools> tools,
    String instructions,
    Boolean store,
    @JsonProperty("previous_response_id")
    String previousResponseId,
    Text text,
    List<Object> input
) {

  @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "FieldMayBeFinal", "FieldCanBeLocal"})
  public static class GptJobsPayloadBuilder {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private List<Object> input = new ArrayList<>();

    private Text text;

    private List<Tools> tools = new ArrayList<>();

    public GptJobsPayloadBuilder addSystemPrompt(String systemPrompt) {
      input.add(new Input("system", List.of(new InputMessage("input_text", systemPrompt))));
      return this;
    }

    public GptJobsPayloadBuilder addUserPrompt(String userPrompt, String fileId) {
      input.add(new Input("user", List.of(
          new InputMessage("input_text", userPrompt),
          new InputFile(fileId)
      )));
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
      return this;
    }

    public GptJobsPayloadBuilder addTools(Tools tool) {
      this.tools.add(tool);
      return this;
    }

  }
}
