package com.jobshunter.dto.grokRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.dto.grokRequest.tools.Tools;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record GrokJobsPayload(
    String model,
    Reasoning reasoning,
    int max_output_tokens,
    UUID previous_response_id,
    List<Tools> tools,
    String instructions,
    Text text,
    Boolean store,
    List<Input> input
) {

  @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "FieldMayBeFinal", "FieldCanBeLocal"})
  public static class GrokJobsPayloadBuilder {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private List<Input> input = new ArrayList<>();

    private Text text;

    private List<Tools> tools = new ArrayList<>();

    public GrokJobsPayloadBuilder addSystemPrompt(String systemPrompt) {
      input.add(new Input("system", List.of(new InputMessage("input_text", systemPrompt))));
      return this;
    }

    public GrokJobsPayloadBuilder addUserPrompt(String userPrompt, String fileId) {
      input.add(new Input("user", List.of(
          new InputMessage("input_text", userPrompt),
          new InputFile(fileId)
      )));
      return this;
    }

    public GrokJobsPayloadBuilder addUserPrompt(String userPrompt) {
      input.add(new Input("user", List.of(
          new InputMessage("input_text", userPrompt)
      )));
      return this;
    }

    public GrokJobsPayloadBuilder setResponseSchema(String schema) {
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

    public GrokJobsPayloadBuilder addTools(Tools tool) {
      this.tools.add(tool);
      return this;
    }

  }
}
