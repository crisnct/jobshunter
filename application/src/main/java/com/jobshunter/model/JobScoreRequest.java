package com.jobshunter.model;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.processor.PackageExpected;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@PackageExpected("com.jobshunter.service.application")
public class JobScoreRequest {

  private AiModelEntity model;

  private String jobDescription;

  private SearchJobOrder order;
}
