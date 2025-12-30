package com.jobshunter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobshunter.dto.serpRequest.WorkType;
import com.jobshunter.model.EngineType;
import com.jobshunter.processor.SqlInjectionSafe;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserUpdateRequest(
    @NotBlank @Size(max = 50) @SqlInjectionSafe String username,
    @NotBlank @Size(max = 30) @SqlInjectionSafe String phoneNumber,
    @NotNull @Positive Integer timeInterval,
    boolean notifyWhatsapp,
    boolean notifyEmail,
    List<SerpPromptUpdate> serpPrompts,
    List<AiPromptUpdate> aiPrompts
) {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record SerpPromptUpdate(
      @Nullable
      @Positive
      Long id,

      @NotBlank(message = "Query must not be blank")
      @Size(max = 2000)
      @JsonProperty("q")
      @SqlInjectionSafe
      String query,

      @Size(max = 255)
      @JsonProperty("google_domain")
      @SqlInjectionSafe
      String googleDomain,

      @Size(max = 128)
      @JsonProperty("date_posted")
      @SqlInjectionSafe
      String datePosted,

      @Size(max = 2)
      @JsonProperty("gl")
      @SqlInjectionSafe
      String country,

      @SqlInjectionSafe
      String location,

      WorkType workType,

      @Nullable
      @Positive
      Integer radius,

      @Size(max = 2)
      @JsonProperty("hl")
      @SqlInjectionSafe
      String language
  ) {

  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record AiPromptUpdate(
      @Nullable
      @Positive
      Long id,

      @SqlInjectionSafe
      @NotBlank
      @Size(max = 3000)
      String prompt,

      @NotNull
      EngineType engine
  ) {

  }
}

