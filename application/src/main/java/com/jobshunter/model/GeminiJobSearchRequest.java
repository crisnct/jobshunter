package com.jobshunter.model;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.AIJobSearchRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeminiJobSearchRequest extends AIJobSearchRequest {

  private String base64CV;

  public GeminiJobSearchRequest(
      String username,
      UserPromptEntity prompt,
      String base64CV
  ) {
    super(username, prompt, prompt.getEngineConfiguration().getEngineType(), prompt.getEngineConfiguration().getTier());
    this.base64CV = base64CV;
  }

}
