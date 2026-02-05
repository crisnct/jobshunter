package com.jobshunter.dto.grokRequest;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.dto.exceptions.BusinessException;
import com.jobshunter.model.AiCapabilityType;
import com.jobshunter.service.AiCapabilityChecker;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Builder
public record GrokScorePayload(
    String model,
    Reasoning reasoning,
    Boolean store,
    Double temperature,
    int max_output_tokens,
    List<Input> input,
    AiModelEntity aiModel
) {

  public static GrokScorePayloadBuilder builder(AiModelEntity aiModel) {
    return new GrokScorePayloadBuilder(aiModel);
  }

  @Slf4j
  @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "FieldMayBeFinal"})
  public static class GrokScorePayloadBuilder {

    private List<Input> input = new ArrayList<>();

    private AiModelEntity aiModel;

    private GrokScorePayloadBuilder(AiModelEntity aiModel) {
      this.aiModel = aiModel;
      this.model = aiModel.getModel();
    }

    public GrokScorePayloadBuilder addSystemPrompt(String systemPrompt) {
      if (AiCapabilityChecker.isEnabled(aiModel, AiCapabilityType.SYSTEM_PROMPT)) {
        input.add(new Input("system", List.of(new InputMessage("input_text", systemPrompt))));
      }
      return this;
    }

    public GrokScorePayloadBuilder reasoning(Reasoning reasoning) {
      if (AiCapabilityChecker.isEnabled(aiModel, AiCapabilityType.REASONING)) {
        this.reasoning = reasoning;
      }
      return this;
    }

    public GrokScorePayloadBuilder temperature(Double temperature) {
      if (AiCapabilityChecker.isEnabled(aiModel, AiCapabilityType.TEMPERATURE)) {
        this.temperature = temperature;
      }
      return this;
    }

    public GrokScorePayloadBuilder addUserPrompt(String userPrompt, String fileId) {
      final List<InputObj> dataList = new ArrayList<>();
      dataList.add(new InputMessage("input_text", userPrompt));
      if (AiCapabilityChecker.isEnabled(aiModel, AiCapabilityType.FILE_UPLOAD)) {
        dataList.add(new InputFile(fileId));
      }
      input.add(new Input("user", dataList));
      return this;
    }

    public GrokScorePayload build() {
      if (reasoning != null && temperature != null) {
        throw new BusinessException(HttpStatus.NOT_FOUND, "TEMPERATURE and REASONING can not be set both for GPT models.");
      }
      return new GrokScorePayload(
          model,
          reasoning,
          store,
          temperature,
          max_output_tokens,
          input,
          aiModel
      );
    }

  }

}

