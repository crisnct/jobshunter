package com.jobshunter.dto;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.model.EngineSelection;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdditionalEffortRequest extends AIJobSearchRequest implements Cloneable {

  private Boolean storeConversation;
  private String prevResponseId;
  private String fileId;
  private List<String> previousURL;

  public AdditionalEffortRequest(UserEntity user, EngineSelection engineSelection) {
    super(user, engineSelection);
  }

  @Override
  public AdditionalEffortRequest clone() {
    AdditionalEffortRequest clone = (AdditionalEffortRequest) super.clone();
    clone.storeConversation = this.storeConversation;
    clone.prevResponseId = this.prevResponseId;
    clone.fileId = this.fileId;
    clone.previousURL = this.previousURL != null ? new ArrayList<>(this.previousURL) : null;
    return clone;
  }
}
