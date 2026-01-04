package com.jobshunter.dto.grokRequest;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

@Builder
public record GrokScorePayload(
    String model,
    Boolean store,
    double temperature,
    int max_output_tokens,
    List<Input> input
) {

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  public static class GrokScorePayloadBuilder {

    private List<Input> input = new ArrayList<>();

    public GrokScorePayloadBuilder addSystemPrompt(String systemPrompt) {
      input.add(new Input("system", List.of(new InputMessage("input_text", systemPrompt))));
      return this;
    }

    public GrokScorePayloadBuilder addUserPrompt(String userPrompt, String fileId) {
      input.add(new Input("user", List.of(
          new InputMessage("input_text", userPrompt),
          new InputFile(fileId)
      )));
      return this;
    }
  }

}

