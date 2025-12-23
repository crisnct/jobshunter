package com.jobshunter.dto.geminiRequest;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.dto.EngineType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeminiJobSearchRequest extends AIJobSearchRequest {

  private String base64CV;

  public GeminiJobSearchRequest(String username, UserPromptEntity prompt, String base64CV, EngineType engine) {
    super(username, prompt, engine);
    this.base64CV = base64CV;
  }

}
