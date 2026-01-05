package com.jobshunter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.model.EngineSelection;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIJobSearchRequest {

  private UserEntity user;
  private EngineSelection engineSelection;
  private long promptId;
  private String userPrompt;
  private boolean searchCompanies;

  public AIJobSearchRequest(UserEntity user, EngineSelection engineSelection) {
    this.user = user;
    this.engineSelection = engineSelection;
  }

}
