package com.jobshunter.dto.geminiRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record Part(
    String text,
    FileData fileData,
    InlineData inlineData,
    FunctionCall functionCall
) {
  public static Part text(String value) {
    return Part.builder().text(value).build();
  }

  public static Part file(String fileUri) {
    return Part.builder().fileData(new FileData(fileUri)).build();
  }

  public static Part inlineData(String mimeType, String base64Data) {
    return Part.builder().inlineData(new InlineData(mimeType, base64Data)).build();
  }
}

