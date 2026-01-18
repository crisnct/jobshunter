package com.jobshunter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jobshunter.model.SearchJobOrder;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class AIJobSearchRequest implements Copyable<AIJobSearchRequest> {

  private SearchJobOrder order;
  private long promptId;
  private String userPrompt;

  public AIJobSearchRequest(SearchJobOrder order) {
    this.order = order;
  }

}
