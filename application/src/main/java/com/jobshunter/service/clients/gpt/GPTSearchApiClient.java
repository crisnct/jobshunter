package com.jobshunter.service.clients.gpt;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.dto.Job;
import java.util.List;

@PackageExpected("com.jobshunter.service.application")
public sealed interface GPTSearchApiClient permits AbstractGptApiClient{

  List<Job> search(String userPrompt, String fileId);

}
