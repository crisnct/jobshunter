package com.jobshunter.dto;

import java.util.List;

public record SearchJobsResponse(List<SearchJobResult> jobsFound) {

}
