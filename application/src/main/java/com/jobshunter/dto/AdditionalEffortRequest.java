package com.jobshunter.dto;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.model.EngineSelection;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdditionalEffortRequest extends AIJobSearchRequest {

  private Boolean storeConversation;
  private String prevResponseId;
  private List<String> previousURL;

  public AdditionalEffortRequest(UserEntity user, EngineSelection engineSelection) {
    super(user, engineSelection);
  }

}
