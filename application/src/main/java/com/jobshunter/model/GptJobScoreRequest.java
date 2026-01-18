package com.jobshunter.model;

import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.dto.gptRequest.Reasoning;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GptJobScoreRequest implements JobScoreRequest {

  private String jobDescription;

  private UserCvEntity userCV;

}
