package com.jobshunter.service.clients;

import com.jobshunter.dto.TokenEstimationRequest;
import com.jobshunter.dto.geminiRequest.Content;
import com.jobshunter.dto.geminiRequest.GeminiJobsPayload;
import com.jobshunter.dto.geminiRequest.Part;
import com.jobshunter.dto.gptRequest.GptJobsPayload;
import com.jobshunter.dto.gptRequest.Input;
import com.jobshunter.dto.gptRequest.InputMessage;
import com.jobshunter.dto.grokRequest.GrokJobsPayload;
import com.jobshunter.dto.grokRequest.InputObj;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TokenEstimationMapper {

  private TokenEstimationMapper() {
  }

  public static TokenEstimationRequest from(GptJobsPayload payload) {
    List<String> prompts = payload.input() == null ? List.of() : payload.input().stream()
        .filter(Input.class::isInstance)
        .map(Input.class::cast)
        .flatMap(in -> in.content().stream())
        .filter(InputMessage.class::isInstance)
        .map(InputMessage.class::cast)
        .map(InputMessage::text)
        .toList();

    return new TokenEstimationRequest(
        prompts,
        Collections.singletonList(payload.tools()),
        payload.text(),
        payload.aiModel(),
        payload.maxOutputTokens()
    );
  }

  public static TokenEstimationRequest from(GeminiJobsPayload payload) {
    List<String> prompts = new ArrayList<>();

    if (payload.systemInstruction() != null && payload.systemInstruction().parts() != null) {
      payload.systemInstruction().parts().stream()
          .map(Part::text)
          .filter(s -> s != null && !s.isBlank())
          .forEach(prompts::add);
    }

    if (payload.contents() != null) {
      payload.contents().stream()
          .filter(c -> c.parts() != null)
          .flatMap((Content c) -> c.parts().stream())
          .map(Part::text)
          .filter(s -> s != null && !s.isBlank())
          .forEach(prompts::add);
    }

    int maxOutputTokens = payload.generationConfig() != null ? payload.generationConfig().getMaxOutputTokens() : 0;
    Object responseSchema = payload.generationConfig() != null ? payload.generationConfig().getResponseJsonSchema() : null;

    return new TokenEstimationRequest(
        prompts,
        Collections.singletonList(payload.tools()),
        responseSchema,
        payload.aiModel(),
        maxOutputTokens
    );
  }

  public static TokenEstimationRequest from(GrokJobsPayload payload) {
    List<String> prompts = new ArrayList<>();
    if (payload.input() != null) {
      for (Object item : payload.input()) {
        if (item instanceof com.jobshunter.dto.grokRequest.Input in) {
          for (InputObj contentObj : in.content()) {
            if (contentObj instanceof com.jobshunter.dto.grokRequest.InputMessage msg) {
              prompts.add(msg.text());
            }
          }
        }
      }
    }

    return new TokenEstimationRequest(
        prompts,
        Collections.singletonList(payload.tools()),
        payload.text(),
        payload.aiModel(),
        payload.maxOutputTokens()
    );
  }
}
