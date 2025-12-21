package com.jobshunter.dto;

import java.util.List;

public record SearchJobsRequest(
    String username,
    int iterations,
    List<EngineType> engines
) {
}
