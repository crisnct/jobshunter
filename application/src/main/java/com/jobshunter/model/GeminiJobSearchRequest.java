package com.jobshunter.model;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.dto.AIJobSearchRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeminiJobSearchRequest extends AIJobSearchRequest {

  private String base64CV;

  public GeminiJobSearchRequest(UserEntity user, AiModelEntity aiModel, String base64CV) {
    super(user, aiModel);
    this.base64CV = base64CV;
  }

  @Override
  public GeminiJobSearchRequest copy() {
    GeminiJobSearchRequest copy = new GeminiJobSearchRequest(this.getUser(), getModel(), this.base64CV);
    // Copy parent fields
    copy.setPromptId(this.getPromptId());
    copy.setUserPrompt(this.getUserPrompt());
    copy.setSearchCompanies(this.isSearchCompanies());
    return copy;
  }

}
