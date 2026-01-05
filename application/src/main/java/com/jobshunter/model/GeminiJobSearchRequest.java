package com.jobshunter.model;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.dto.AIJobSearchRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeminiJobSearchRequest extends AIJobSearchRequest {

  private String base64CV;

  public GeminiJobSearchRequest(UserEntity user, EngineSelection engineSelection, String base64CV) {
    super(user, engineSelection);
    this.base64CV = base64CV;
  }

}
