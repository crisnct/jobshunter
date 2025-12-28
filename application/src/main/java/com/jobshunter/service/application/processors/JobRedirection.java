package com.jobshunter.service.application.processors;

import com.jobshunter.service.application.JobContext;
import com.jobshunter.service.application.JobPhase;
import com.jobshunter.service.clients.BrowserSimulator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class JobRedirection implements JobProcessor {

  private final BrowserSimulator browserSimulator;

  @Override
  public JobContext processAsync(JobContext context) {
    String newURL = browserSimulator.getFinalRedirectedURL(context.getJob().getUrl());
    context.getJob().setUrl(newURL);
    context.setPhase(JobPhase.REDIRECTED);
    return context;
  }

}
