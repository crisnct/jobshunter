package com.jobshunter.service.application.hunting;

import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public sealed interface JobHunting permits GenericJobHunting {

  CompletableFuture<List<Job>> searchJobsAsync(SearchJobOrder order);

}
