package com.jobshunter.model;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.dto.AdditionalEffortRequest;
import com.jobshunter.dto.CompanyDto;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GrokJobSearchRequest extends AdditionalEffortRequest {

  private List<CompanyDto> companies;

  public GrokJobSearchRequest(UserEntity user, EngineSelection engineSelection) {
    super(user, engineSelection);
  }

}
