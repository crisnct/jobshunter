package com.jobshunter.model;

import com.jobshunter.database.entities.UserCvEntity;
import lombok.Data;

@Data
public class GptJobScoreRequest implements JobScoreRequest {

  private String jobDescription;

  private UserCvEntity userCV;

}
