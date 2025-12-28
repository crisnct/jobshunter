package com.jobshunter.service.clients.gemini;

import com.jobshunter.model.JobScoreRequest;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GeminiJobScoreRequest implements JobScoreRequest {

  private String resumeFileId;

  private String jobDescriptionFileId;

}
