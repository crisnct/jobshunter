package com.jobshunter.service.clients.gpt;

import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.service.clients.JobScoreRequest;
import lombok.Data;

@Data
public class GptJobScoreRequest implements JobScoreRequest {

  private String jobDescription;

  private UserCvEntity userCV;

}
