package com.jobshunter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.model.EngineTier;
import com.jobshunter.model.EngineType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIJobSearchRequest {

  private String username;

  private UserPromptEntity prompt;

  private EngineType engineType;

  private EngineTier engineTier;

}
