package com.jobshunter.dto.gptRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.dto.exceptions.BusinessException;
import com.jobshunter.model.AiCapabilityType;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Builder
public record GptScorePayload(
    String model,
    Reasoning reasoning,
    Double temperature,
    Boolean store,
    @JsonProperty("max_output_tokens")
    int maxOutputTokens,
    List<Input> input,
    AiModelEntity aiModel
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

    public GptScorePayloadBuilder temperature(Double temperature) {
      if (isEnabledCapability(AiCapabilityType.TEMPERATURE)) {
        this.temperature = temperature;
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

    public GptScorePayload build() {
      if (reasoning != null && temperature != null) {
        throw new BusinessException(HttpStatus.NOT_FOUND, "TEMPERATURE and REASONING can not be set both for GPT models.");
      }
      return new GptScorePayload(
          model,
          reasoning,
          temperature,
          store,
          maxOutputTokens,
          input,
          aiModel
      );
    }

  }

}

