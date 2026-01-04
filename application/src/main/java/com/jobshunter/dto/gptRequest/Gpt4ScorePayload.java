package com.jobshunter.dto.gptRequest;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

@Builder
public record Gpt4ScorePayload(
    String model,
    double temperature,
    Boolean store,
    int max_output_tokens,
    List<Input> input
) {

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  public static class Gpt4ScorePayloadBuilder {

    private List<Input> input = new ArrayList<>();

    public Gpt4ScorePayload.Gpt4ScorePayloadBuilder addSystemPrompt(String systemPrompt) {
      input.add(new Input("system", List.of(new InputMessage("input_text", systemPrompt))));
      return this;
    }

    public Gpt4ScorePayload.Gpt4ScorePayloadBuilder addUserPrompt(String userPrompt, String fileId) {
      input.add(new Input("user", List.of(
          new InputMessage("input_text", userPrompt),
          new InputFile(fileId)
      )));
      return this;
    }
  }

}

