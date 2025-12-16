package com.jobshunter.dto;

public record SearchJobsRequest(String username, Boolean notifyOnWhatsApp, int iterations) {
}
