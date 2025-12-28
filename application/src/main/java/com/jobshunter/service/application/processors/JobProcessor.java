package com.jobshunter.service.application.processors;

import com.jobshunter.service.application.JobContext;

public interface JobProcessor {

    JobContext processAsync(JobContext job);
}
