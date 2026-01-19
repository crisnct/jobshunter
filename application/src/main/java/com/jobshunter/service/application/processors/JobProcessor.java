package com.jobshunter.service.application.processors;

import com.jobshunter.model.JobContext;

sealed interface JobProcessor permits JobBasicCheckProcessor, JobBodyExtractorProcessor, JobFetchProcessor, JobScoringProcessor,
    JobValidatorProcessor {

  JobContext processAsync(JobContext job);

}
