package com.jobshunter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.UserEntity;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class AIJobSearchRequest implements Copyable<AIJobSearchRequest> {

  private UserEntity user;
  private AiModelEntity model;
  private long promptId;
  private String userPrompt;
  private boolean searchCompanies;

  public AIJobSearchRequest(UserEntity user, AiModelEntity model) {
    this.user = user;
    this.model = model;
  }

}
