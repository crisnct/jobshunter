package com.jobshunter.service.clients;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.gpt.GptV1JobSearchImpl;
import com.jobshunter.service.clients.grok.GrokV1JobSearchImpl;

@PackageExpected("com.jobshunter.service.application")
public sealed interface DeleteConvAiClient permits GptV1JobSearchImpl, GrokV1JobSearchImpl {

  void deleteConversation(String id);

}
