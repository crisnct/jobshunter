package com.jobshunter.model;

import com.jobshunter.database.entities.UserEntity;
import lombok.Data;

@Data
public class SearchJobOrder {

  private UserEntity user;
  private EngineSelection engineSelection;
  private boolean searchByUserPrompt;
  private boolean searchCompanies;

  public SearchJobOrder() {

  }

}
