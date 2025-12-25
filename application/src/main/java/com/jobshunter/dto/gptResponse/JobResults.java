package com.jobshunter.dto.gptResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jobshunter.model.Job;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobResults(Job[] results) {

}
