package com.jobshunter.dto.grokRequest;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.model.AiCapabilityType;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Builder
public record GrokScorePayload(
    String model,
    Reasoning reasoning,
    Boolean store,
    double temperature,
    int max_output_tokens,
    List<Input> input
) {

  public static GrokScorePayloadBuilder builder(AiModelEntity aiModel) {
    return new GrokScorePayloadBuilder(aiModel);
  }

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  @Slf4j
  public static class GrokScorePayloadBuilder {

    private List<Input> input = new ArrayList<>();

    private AiModelEntity aiModel;

    private GrokScorePayloadBuilder(AiModelEntity aiModel) {
      this.aiModel = aiModel;
      this.model = aiModel.getModel();
    }

    public GrokScorePayloadBuilder addSystemPrompt(String systemPrompt) {
      if (isEnabledCapability(AiCapabilityType.SYSTEM_PROMPT)) {
        input.add(new Input("system", List.of(new InputMessage("input_text", systemPrompt))));
      }
      return this;
    }

    public GrokScorePayloadBuilder reasoning(Reasoning reasoning) {
      if (isEnabledCapability(AiCapabilityType.REASONING)) {
        this.reasoning = reasoning;
      }
      return this;
    }

    public GrokScorePayloadBuilder addUserPrompt(String userPrompt, String fileId) {
      input.add(new Input("user", List.of(
          new InputMessage("input_text", userPrompt),
          new InputFile(fileId)
      )));
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

