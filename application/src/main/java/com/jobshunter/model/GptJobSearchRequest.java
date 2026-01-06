package com.jobshunter.model;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.dto.AdditionalEffortRequest;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.gptRequest.Reasoning;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GptJobSearchRequest extends AdditionalEffortRequest {

  private final Reasoning reasoning;
  private List<CompanyDto> companies;

  public GptJobSearchRequest(UserEntity user, EngineSelection engineSelection, Reasoning reasoning) {
    super(user, engineSelection);
    this.reasoning = reasoning;
  }

}
