package com.jobshunter.service.application.processors;

import com.jobshunter.model.JobContext;

public interface JobProcessor {

    JobContext processAsync(JobContext job);
}
