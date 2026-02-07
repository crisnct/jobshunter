package com.jobshunter.service.application.hunting;

import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public sealed interface JobByCompanyHunting extends JobHunting
    permits GeminiJobHunting, GptJobHunting, GrokJobHunting {

  CompletableFuture<List<Job>> searchJobsByCompaniesAsync(SearchJobOrder order);

}
