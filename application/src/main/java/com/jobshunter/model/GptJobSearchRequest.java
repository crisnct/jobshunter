package com.jobshunter.model;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.dto.gptRequest.Reasoning;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GptJobSearchRequest extends AIJobSearchRequest {

  private final String fileId;

  private final Reasoning reasoning;

  public GptJobSearchRequest(
      String username,
      UserPromptEntity prompt,
      String fileId,
      Reasoning reasoning
  ) {
    super(username, prompt);
    this.fileId = fileId;
    this.reasoning = reasoning;
  }

}
