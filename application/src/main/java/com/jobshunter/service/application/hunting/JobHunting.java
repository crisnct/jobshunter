package com.jobshunter.service.application.hunting;

import com.jobshunter.dto.SearchJobOrder;
import com.jobshunter.service.application.JobsSynchronizer;
import java.util.concurrent.CompletableFuture;

public sealed interface JobHunting permits GeminiJobHunting, GptJobHunting, SerpJobHunting {

  CompletableFuture<Void> searchJobs(JobsSynchronizer jobsSync, SearchJobOrder order);

}
