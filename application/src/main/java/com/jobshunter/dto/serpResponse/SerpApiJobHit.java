package com.jobshunter.dto.serpResponse;

import java.util.List;

public record SerpApiJobHit(
    String title,
    String company,
    String location,
    String description,
    String highlights,
    String jobId,
    List<String> applyLinks
) {

}