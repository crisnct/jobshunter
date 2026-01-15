package com.jobshunter.dto;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.model.EngineSelection;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AdditionalEffortRequest extends AIJobSearchRequest {

  private Boolean storeConversation;
  private String prevResponseId;
  private String fileId;
  private List<String> previousURL;

  public AdditionalEffortRequest(UserEntity user, EngineSelection engineSelection) {
    super(user, engineSelection);
  }

}
