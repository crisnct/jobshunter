package com.jobshunter.dto.geminiRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record GeminiJobsPayload(
    List<Content> contents,
    Content system_instruction,
    GenerationConfig generationConfig,
    List<SafetySetting> safetySettings,
    List<Tool> tools,
    ToolConfig toolConfig
) {

  public static class GeminiJobsPayloadBuilder {

    private Content system_instruction;

    private List<Content> contents = new ArrayList<>();

    public GeminiJobsPayloadBuilder addSystemInstruction(String text) {
      this.system_instruction = new Content(null, List.of(Part.text(text)));
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
      for (FileData fileData : files) {
        parts.add(Part.file(fileData.fileUri(), fileData.mimeType()));
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
