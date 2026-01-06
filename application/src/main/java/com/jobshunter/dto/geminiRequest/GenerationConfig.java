package com.jobshunter.dto.geminiRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record GenerationConfig(
    Double temperature,
    Double topP,
    Integer topK,
    Integer maxOutputTokens,
    Integer candidateCount,
    Double presencePenalty,
    Double frequencyPenalty,
    List<String> stopSequences,
    String responseMimeType,
    Object responseJsonSchema,
    ThinkingConfig thinkingConfig
) {

}
