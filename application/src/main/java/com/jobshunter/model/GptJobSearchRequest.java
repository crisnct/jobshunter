package com.jobshunter.model;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.gptRequest.Reasoning;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GptJobSearchRequest extends AIJobSearchRequest {

  private String model;

  private List<CompanyDto> companies;

  private final Reasoning reasoning;

  public GptJobSearchRequest(
      UserEntity user,
      UserPromptEntity prompt,
      Reasoning reasoning
  ) {
    super(user, prompt);
    this.reasoning = reasoning;
  }

}
