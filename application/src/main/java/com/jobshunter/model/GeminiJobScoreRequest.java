package com.jobshunter.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GeminiJobScoreRequest implements JobScoreRequest {

  private String resumeFileId;

  private String jobDescription;

}
