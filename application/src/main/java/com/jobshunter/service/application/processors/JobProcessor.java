package com.jobshunter.service.application.processors;

import com.jobshunter.model.JobContext;
import com.jobshunter.service.application.processors.validation.JobValidatorProcessor;

public sealed interface JobProcessor permits JobBasicCheckProcessor, JobBodyExtractorProcessor, JobFetchProcessor, JobScoringProcessor,
    JobValidatorProcessor {

  JobContext processAsync(JobContext job);

}
