package com.jobshunter.service.application.processors;

import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobPhase;
import com.jobshunter.service.clients.BrowserSimulator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class JobRedirection implements JobProcessor {

  private final BrowserSimulator browserSimulator;

  @Override
  public JobContext processAsync(JobContext context) {
    log.info("Checking redirection for job URL: {}", StringUtils.abbreviate(context.getJob().getUrl(), 50));
    String newURL = browserSimulator.getFinalRedirectedURL(context.getJob().getUrl());
    context.getJob().setUrl(newURL);
    context.setPhase(JobPhase.REDIRECTED);
    return context;
  }

}
