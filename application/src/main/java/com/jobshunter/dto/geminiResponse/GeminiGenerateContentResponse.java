package com.jobshunter.dto.geminiResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jobshunter.dto.geminiRequest.Content;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiGenerateContentResponse(
    List<Candidate> candidates,
    UsageMetadata usageMetadata,
    String modelVersion,
    String responseId
) {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Candidate(
      Content content,
      String finishReason,
      Integer index
  ) {
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record UsageMetadata(
      Integer promptTokenCount,
      Integer candidatesTokenCount,
      Integer totalTokenCount,
      List<PromptTokensDetail> promptTokensDetails
  ) {
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PromptTokensDetail(
      String modality,
      Integer tokenCount
  ) {
  }
}

