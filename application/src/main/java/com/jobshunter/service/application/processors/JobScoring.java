package com.jobshunter.service.application.processors;

import com.jobshunter.model.JobScoreRequest;

public sealed interface JobScoring<T extends JobScoreRequest> extends JobProcessor permits JobScoringGemini, JobScoringGrok {

}
