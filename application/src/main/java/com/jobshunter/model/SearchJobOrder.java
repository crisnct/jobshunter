package com.jobshunter.model;

import com.jobshunter.database.entities.UserEntity;
import lombok.Data;

@Data
public class SearchJobOrder {

  private UserEntity user;
  private boolean searchCompanies;
  private boolean searchByPrompts;
  private EngineSelection engineSelection;

  public SearchJobOrder() {

  }

}
