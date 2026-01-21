package com.jobshunter.model;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.dto.IpInfoDetailResponse;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
//TODO remove the class and use JobOrderEntity after following TODO is done
public class SearchJobOrder {

  private final UserEntity user;
  private final AiModelEntity model;
  private final boolean searchByUserPrompt;
  private final boolean searchCompanies;
  private final JobOrderEntity jobOrder;
  private final List<String> ignoredURLs;

  @Setter
  //TODO move it to login functionality. When the user login get the IP info and store country and city in the database in user_session table in new fields
  private IpInfoDetailResponse ipInfo;

  public SearchJobOrder(JobOrderEntity jobOrder, UserEntity user, List<String> ignoredURLs) {
    this.jobOrder = jobOrder;
    this.user = user;
    this.searchCompanies = jobOrder.isSearchCompanies();
    this.searchByUserPrompt = jobOrder.isSearchByPrompts();
    this.model = jobOrder.getAiModel();
    this.ignoredURLs = ignoredURLs;
  }

}
