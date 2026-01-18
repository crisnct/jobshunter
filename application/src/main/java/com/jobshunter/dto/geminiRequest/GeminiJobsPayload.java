package com.jobshunter.dto.geminiRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.model.AiCapabilityType;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record GeminiJobsPayload(
    List<Content> contents,
    @JsonProperty("system_instruction")
    Content systemInstruction,
    GenerationConfig generationConfig,
    List<SafetySetting> safetySettings,
    List<Tool> tools,
    ToolConfig toolConfig
) {

  public static GeminiJobsPayloadBuilder builder(AiModelEntity model) {
    return new GeminiJobsPayloadBuilder(model);
  }

  @Slf4j
  @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal", "MismatchedQueryAndUpdateOfCollection", "unused"})
  public static class GeminiJobsPayloadBuilder {

    private Content systemInstruction;

    private List<Content> contents = new ArrayList<>();

    private AiModelEntity aiModel;

    private GeminiJobsPayloadBuilder(AiModelEntity model) {
      this.aiModel = model;
    }

    private boolean isEnabledCapability(AiCapabilityType type) {
      boolean enabledCapability = aiModel.isEnabledCapability(type);
      if (!enabledCapability) {
        log.debug(type + " capability is not supported by model " + aiModel.getModel());
      }
      return enabledCapability;
    }

    public GeminiJobsPayloadBuilder tools(List<Tool> tools) {
      if (isEnabledCapability(AiCapabilityType.WEB_SEARCH)) {
        this.tools = tools;
      }
      return this;
    }

    public GeminiJobsPayloadBuilder addSystemInstruction(String text) {
      if (isEnabledCapability(AiCapabilityType.SYSTEM_PROMPT)) {
        this.systemInstruction = new Content(null, List.of(Part.text(text)));
      }
      return this;
    }

    public GeminiJobsPayloadBuilder addUserContent(String text, String mimeType, String base64Data) {
      List<Part> parts = new ArrayList<>();
      if (text != null && !text.isBlank()) {
        parts.add(Part.text(text));
      }
      if (mimeType != null && !mimeType.isBlank() && base64Data != null && !base64Data.isBlank()) {
        parts.add(Part.inlineData(mimeType, base64Data));
      }
      if (!parts.isEmpty()) {
        this.contents.add(new Content("user", parts));
      }
      return this;
    }

    public GeminiJobsPayloadBuilder addUserContent(String text, List<FileData> files) {
      List<Part> parts = new ArrayList<>();
      if (text != null && !text.isBlank()) {
        parts.add(Part.text(text));
      }
      if (isEnabledCapability(AiCapabilityType.FILE_UPLOAD)) {
        for (FileData fileData : files) {
          parts.add(Part.file(fileData.fileUri(), fileData.mimeType()));
        }
      }
      if (!parts.isEmpty()) {
        this.contents.add(new Content("user", parts));
      }
      return this;
    }

    public GeminiJobsPayloadBuilder addUserContent(String text) {
      List<Part> parts = new ArrayList<>();
      if (text != null && !text.isBlank()) {
        parts.add(Part.text(text));
      }
      if (!parts.isEmpty()) {
        this.contents.add(new Content("user", parts));
      }
      return this;
    }

    public GeminiJobsPayloadBuilder addModelContent(String text) {
      List<Part> parts = new ArrayList<>();
      if (text != null && !text.isBlank()) {
        parts.add(Part.text(text));
      }
      if (!parts.isEmpty()) {
        this.contents.add(new Content("model", parts));
      }
      return this;
    }

  }

}
