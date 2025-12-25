package com.jobshunter.dto;

import com.jobshunter.model.Job;
import java.util.List;

public record JobHuntResponse(List<Job> jobsFound) {

}
