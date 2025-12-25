package com.jobshunter.dto;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.model.EngineTier;
import com.jobshunter.model.EngineType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AIJobSearchRequest {

  private String username;

  private UserPromptEntity prompt;

  private EngineType engineType;

  private EngineTier engineTier;

}
