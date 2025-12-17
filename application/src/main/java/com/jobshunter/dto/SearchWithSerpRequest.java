package com.jobshunter.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

public record SearchWithSerpRequest(
    @NotBlank(message = "Query must not be blank") String query,

    /// Possible values:
    /// {@snippet :
    ///   google.cl
    ///   google.ro
    ///   google.com
    ///   google.pt
    ///   .....
    ///}
    //See more here https://serpapi.com/google-domains
    @Nullable String googleDomain,

    /// Possible values:
    /// {@snippet :
    ///   today
    ///   last_3_days
    ///   last_7_days
    ///   last_14_days
    ///   last_30_days
    ///}
    @Nullable String datePosted,

    // Possible values: uk, ro, ca, us, ...
    //See more here https://serpapi.com/google-countries
    @Nullable String country,

    // Possible values: en, ro, de, ...
    // See more here https://serpapi.com/google-languages
    @Nullable String language
) {

}