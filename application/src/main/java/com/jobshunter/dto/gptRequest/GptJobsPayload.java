package com.jobshunter.dto.gptRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jobshunter.dto.gptRequest.tools.Tools;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record GptJobsPayload(
    String model,
    Reasoning reasoning,
    int max_output_tokens,
    List<Tools> tools,
    String instructions,
    Text text,
    List<Input> input
) {

  @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "FieldMayBeFinal", "FieldCanBeLocal"})
  public static class GptJobsPayloadBuilder {

    private List<Input> input = new ArrayList<>();

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

    public GptJobsPayloadBuilder setResponseSchema(Object schema) {
      text = new Text(schema);
      return this;
    }

    public GptJobsPayloadBuilder addTools(Tools tool) {
      this.tools.add(tool);
      return this;
    }

  }
}
