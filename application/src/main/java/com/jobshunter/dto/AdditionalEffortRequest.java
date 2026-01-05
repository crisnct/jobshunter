package com.jobshunter.dto;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.model.EngineSelection;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdditionalEffortRequest extends AIJobSearchRequest {

  private Boolean store;
  private String prevResponseId;

  public AdditionalEffortRequest(UserEntity user, EngineSelection engineSelection) {
    super(user, engineSelection);
  }

}
