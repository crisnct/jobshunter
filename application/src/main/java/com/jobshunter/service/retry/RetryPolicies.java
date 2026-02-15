package com.jobshunter.service.retry;

import com.jobshunter.dto.CompanyDto;
import com.jobshunter.model.AiClientResponse;
import java.util.List;

public final class RetryPolicies {

  public static final RetryPolicy<AiClientResponse> JOB_SEARCH =
      new RetryPolicy<>(
          "JOB_SEARCH",
          3,
          2_000,
          r -> r != null && r.getJobs() != null && !r.getJobs().isEmpty(),
          ex -> ex instanceof RuntimeException,
          new AiClientResponse()
      );

  public static final RetryPolicy<AiClientResponse> JOB_SEARCH_BY_COMPANY =
      new RetryPolicy<>(
          "JOB_SEARCH",
          1,
          1_000,
          r -> r != null && r.getJobs() != null && !r.getJobs().isEmpty(),
          ex -> ex instanceof RuntimeException,
          new AiClientResponse()
      );

  public static final RetryPolicy<List<CompanyDto>> COMPANY_SEARCH =
      new RetryPolicy<>(
          "COMPANY_SEARCH",
          3,
          1_000,
          r -> r != null && !r.isEmpty(),
          ex -> ex instanceof RuntimeException,
          List.of()
      );

  private RetryPolicies() {
  }
}
