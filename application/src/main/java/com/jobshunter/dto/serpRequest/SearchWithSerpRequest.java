package com.jobshunter.dto.serpRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchWithSerpRequest(
    @NotBlank(message = "Query must not be blank")
    @Size(max = 2000)
    @JsonProperty("q")
    String query,

    /// Possible values:
    /// {@snippet :
    ///   google.cl
    ///   google.ro
    ///   google.com
    ///   google.pt
    ///   .....
    ///}
    //See more here https://serpapi.com/google-domains
    @Size(max = 255)
    @JsonProperty("google_domain")
    @Nullable
    String googleDomain,

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
    String datePosted,

    // Possible values: uk, ro, ca, us, ...
    //See more here https://serpapi.com/google-countries
    @JsonProperty("gl")
    @Size(max = 2)
    @Nullable
    String country,

    // Possible values: en, ro, de, ...
    // See more here https://serpapi.com/google-languages
    @JsonProperty("hl")
    @Size(max = 2)
    @Nullable
    String language
) {

}