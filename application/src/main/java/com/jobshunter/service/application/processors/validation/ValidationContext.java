package com.jobshunter.service.application.processors.validation;

import com.jobshunter.model.ContractType;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobType;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Context containing pre-calculated data for job validation rules.
 */
@Getter
@Builder
public class ValidationContext {

  private final String html;
  private final JobContext jobContext;
  private final boolean cityMatch;
  private final boolean countryMatch;
  private final boolean freelancerRole;
  private final boolean remoteRole;
  private final boolean localJob;

  public List<JobType> getUserJobTypes() {
    return jobContext.getUserJobTypes();
  }

  public List<ContractType> getUserContractTypes() {
    return jobContext.getUserContractTypes();
  }

}
