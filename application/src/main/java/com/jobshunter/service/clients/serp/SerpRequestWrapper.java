package com.jobshunter.service.clients.serp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.model.WorkType;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SerpRequestWrapper {

  @NotBlank(message = "Query must not be blank")
  @Size(max = 2000)
  @JsonProperty("q")
  private String query;

  /// Possible values: google.cl google.ro google.com .....
  ///
  //See more here https://serpapi.com/google-domains
  @Size(max = 255)
  @JsonProperty("google_domain")
  @Nullable
  private String googleDomain;

  /// Possible values:
  /// {@snippet :
  ///   today
  ///   last_3_days
  ///   last_7_days
  ///   last_14_days
  ///   last_30_days
  ///}
  @JsonProperty("date_posted")
  @Size(max = 128)
  @Nullable
  private String datePosted;

  // Possible values: uk, ro, ca, us, ...
  //See more here https://serpapi.com/google-countries
  @JsonProperty("gl")
  @Size(max = 2)
  @Nullable
  private String country;

  @Nullable
  private String location;

  @Nullable
  private WorkType workType;

  //Radius in kilometers where to look for job
  @Nullable
  private Integer radius;

  // Possible values: en, ro, de, ...
  // See more here https://serpapi.com/google-languages
  @JsonProperty("hl")
  @Size(max = 2)
  @Nullable
  private String language;

  private SearchJobOrder order;

}