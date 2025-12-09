package com.jobshunter.service.clients.jobSearch;

import com.jobshunter.dto.Job;
import java.util.List;

public sealed interface GPTSearchApiClient permits ChatGptApi4Client, ChatGptApi5Client{

  List<Job> search(String systemPrompt, String userPrompt, String fileId);

}
