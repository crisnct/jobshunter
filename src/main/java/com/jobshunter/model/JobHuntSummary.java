package com.jobshunter.model;

import java.util.List;

public record JobHuntSummary(String prompt,
                              String cvPath,
                              List<JobOpportunity> jobsFound) {
}
