package com.jobshunter.model;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.AIJobSearchRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GptJobSearchRequest extends AIJobSearchRequest {

  private String fileId;

  public GptJobSearchRequest(
      String username,
      UserPromptEntity prompt,
      String fileId
  ) {
    super(username, prompt, prompt.getEngineConfiguration().getEngineType(), prompt.getEngineConfiguration().getTier());
    this.fileId = fileId;
  }

}
