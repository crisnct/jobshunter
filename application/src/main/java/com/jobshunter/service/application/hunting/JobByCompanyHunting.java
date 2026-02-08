package com.jobshunter.service.application.hunting;

import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.hunting.hunters.GeminiJobHunting;
import com.jobshunter.service.application.hunting.hunters.GptJobHunting;
import com.jobshunter.service.application.hunting.hunters.GrokJobHunting;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public sealed interface JobByCompanyHunting
    permits GeminiJobHunting, GptJobHunting, GrokJobHunting {

  CompletableFuture<List<Job>> searchJobsByCompaniesAsync(SearchJobOrder order);

}
