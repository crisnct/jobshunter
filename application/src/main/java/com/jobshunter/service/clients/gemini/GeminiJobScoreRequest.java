package com.jobshunter.service.clients.gemini;

import com.jobshunter.service.clients.JobScoreRequest;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GeminiJobScoreRequest implements JobScoreRequest {

  private String resumeFileId;

  private String jobDescriptionFileId;

}
