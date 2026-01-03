package com.jobshunter.model;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.grokRequest.Reasoning;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GrokJobSearchRequest extends AIJobSearchRequest {

  private List<CompanyDto> companies;

  private final Reasoning reasoning;

  public GrokJobSearchRequest(
      SearchJobOrder order,
      UserPromptEntity prompt,
      Reasoning reasoning
  ) {
    super(order, prompt);
    this.reasoning = reasoning;
  }

}
