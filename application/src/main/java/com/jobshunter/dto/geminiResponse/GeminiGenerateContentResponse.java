package com.jobshunter.dto.geminiResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobshunter.dto.geminiRequest.Content;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiGenerateContentResponse(
    List<Candidate> candidates,
    @JsonProperty("usageMetadata") UsageMetadata usageMetadata,
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
      @JsonProperty("promptTokenCount") Integer promptTokenCount,
      @JsonProperty("candidatesTokenCount") Integer candidatesTokenCount,
      @JsonProperty("totalTokenCount") Integer totalTokenCount,
      @JsonProperty("promptTokensDetails") List<PromptTokensDetail> promptTokensDetails
  ) {

  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PromptTokensDetail(
      @JsonProperty("modality") String modality,
      @JsonProperty("tokenCount") Integer tokenCount
  ) {

  }
}

