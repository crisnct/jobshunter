package com.jobshunter.model;

import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.dto.IpInfoDetailResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
public class SearchJobOrder {

  private final UserEntity user;
  private final EngineSelection engineSelection;
  private final boolean searchByUserPrompt;
  private final boolean searchCompanies;
  private final JobOrderEntity jobOrder;
  @Setter
  private IpInfoDetailResponse ipInfo;

  public SearchJobOrder(JobOrderEntity jobOrder) {
    this.jobOrder = jobOrder;
    this.user = jobOrder.getUser();
    this.searchCompanies = jobOrder.isSearchCompanies();
    this.searchByUserPrompt = jobOrder.isSearchByPrompts();
    this.engineSelection = new EngineSelection(jobOrder.getAiModel().getProvider(), jobOrder.getAiModel().getModel());
  }

}
