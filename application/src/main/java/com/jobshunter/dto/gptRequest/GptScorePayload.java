package com.jobshunter.dto.gptRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.model.AiCapabilityType;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Builder
public record GptScorePayload(
    String model,
    Reasoning reasoning,
    double temperature,
    Boolean store,
    @JsonProperty("max_output_tokens")
    int maxOutputTokens,
    List<Input> input
) {


  public static GptScorePayloadBuilder builder(AiModelEntity aiModel) {
    return new GptScorePayloadBuilder(aiModel);
  }

  @Slf4j
  @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "FieldMayBeFinal"})
  public static class GptScorePayloadBuilder {

    private AiModelEntity aiModel;

    private List<Input> input = new ArrayList<>();

    public GptScorePayloadBuilder(AiModelEntity aiModel) {
      this.aiModel = aiModel;
      this.model = aiModel.getModel();
    }

    public GptScorePayloadBuilder reasoning(Reasoning reasoning) {
      if (isEnabledCapability(AiCapabilityType.REASONING)) {
        this.reasoning = reasoning;
      }
      return this;
    }

    public GptScorePayloadBuilder addSystemPrompt(String systemPrompt) {
      if (isEnabledCapability(AiCapabilityType.SYSTEM_PROMPT)) {
        input.add(new Input("system", List.of(new InputMessage("input_text", systemPrompt))));
      }
      return this;
    }

    public GptScorePayloadBuilder addUserPrompt(String userPrompt, String fileId) {
      final List<InputObj> dataList = new ArrayList<>();
      dataList.add(new InputMessage("input_text", userPrompt));
      if (isEnabledCapability(AiCapabilityType.FILE_UPLOAD)) {
        dataList.add(new InputFile(fileId));
      }
      input.add(new Input("user", dataList));
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

