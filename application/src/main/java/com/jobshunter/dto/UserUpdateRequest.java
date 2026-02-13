package com.jobshunter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jobshunter.model.ContractType;
import com.jobshunter.model.JobType;
import com.jobshunter.model.Relocation;
import com.jobshunter.processor.SqlInjectionSafe;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserUpdateRequest(
    @NotBlank @Size(max = 50) @SqlInjectionSafe String username,
    @NotBlank @Size(max = 30) @SqlInjectionSafe String phoneNumber,
    boolean notifyWhatsapp,
    boolean notifyEmail,
    List<AiPromptUpdate> aiPrompts,
    @Nullable @Size(max = 64) @SqlInjectionSafe String city,
    @Nullable @Size(max = 64) @SqlInjectionSafe String country,
    @Nullable @Size(max = 64) @SqlInjectionSafe String jobDomain,

    @Nullable List<String> jobRoles,
    @Nullable List<JobType> jobTypes,
    @Nullable Relocation relocation,
    @Nullable List<ContractType> contractTypes,
    // [Issue #46] User's declared languages for job filtering
    @Nullable List<String> languages
) {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record AiPromptUpdate(
      @SqlInjectionSafe
      @NotBlank
      @Size(max = 3000)
      String prompt
  ) {

  }
}

