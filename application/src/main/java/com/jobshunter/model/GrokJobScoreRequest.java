package com.jobshunter.model;

import com.jobshunter.database.entities.UserCvEntity;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GrokJobScoreRequest implements JobScoreRequest {

  private String jobDescription;

  private UserCvEntity userCV;

}
