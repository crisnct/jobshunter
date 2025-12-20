package com.jobshunter.dto;

public record SearchJobsRequest(String username, String gptModel, int iterations) {
}
