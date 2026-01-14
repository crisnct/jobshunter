package com.jobshunter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.model.EngineSelection;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIJobSearchRequest implements Cloneable {

  private UserEntity user;
  private EngineSelection engineSelection;
  private long promptId;
  private String userPrompt;
  private boolean searchCompanies;

  public AIJobSearchRequest(UserEntity user, EngineSelection engineSelection) {
    this.user = user;
    this.engineSelection = engineSelection;
  }

  @Override
  public AIJobSearchRequest clone() {
    try {
      AIJobSearchRequest clone = (AIJobSearchRequest) super.clone();
      // super.clone() already performs shallow copy of all fields
      // EngineSelection is recreated to ensure immutability
      clone.engineSelection = new EngineSelection(
          this.engineSelection.type(),
          this.engineSelection.model()
      );
      // UserEntity is shared (immutable-like), no need to clone
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }

}
