package com.jobshunter.model;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.UserEntity;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
public class SearchJobOrder {

  private final UserEntity user;
  private final AiModelEntity model;
  private final boolean searchByUserPrompt;
  private final boolean searchCompanies;
  private final JobOrderEntity jobOrder;
  private final List<String> ignoredURLs;
  @Setter
  private String countryISOcode;

  public SearchJobOrder(JobOrderEntity jobOrder, UserEntity user, List<String> ignoredURLs) {
    this.jobOrder = jobOrder;
    this.user = user;
    this.searchCompanies = jobOrder.isSearchCompanies();
    this.searchByUserPrompt = jobOrder.isSearchByPrompts();
    this.model = jobOrder.getAiModel();
    this.ignoredURLs = ignoredURLs;
  }

}
