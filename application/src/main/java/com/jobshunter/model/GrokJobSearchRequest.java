package com.jobshunter.model;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.dto.AdditionalEffortRequest;
import com.jobshunter.dto.CompanyDto;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GrokJobSearchRequest extends AdditionalEffortRequest implements Cloneable {

  private List<CompanyDto> companies;

  public GrokJobSearchRequest(UserEntity user, EngineSelection engineSelection) {
    super(user, engineSelection);
  }

  @Override
  public GrokJobSearchRequest clone() {
    GrokJobSearchRequest clone = (GrokJobSearchRequest) super.clone();
    clone.companies = this.companies != null ? new ArrayList<>(this.companies) : null;
    return clone;
  }

}
