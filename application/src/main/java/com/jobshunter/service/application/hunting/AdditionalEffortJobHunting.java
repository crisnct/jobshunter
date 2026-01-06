package com.jobshunter.service.application.hunting;

import com.jobshunter.dto.AdditionalEffortRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.PromptType;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.clients.AiJobsClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class AdditionalEffortJobHunting<T extends AdditionalEffortRequest> extends GenericJobHunting<T> {

  private final List<String> additionalPrompts;

  public AdditionalEffortJobHunting(Executor executor,
      AiJobsClient<T, AiClientResponse> jobsClient,
      UserCvService userCvService,
      TemplateRenderer templateRenderer
  ) {
    super(executor, jobsClient, userCvService);
    this.additionalPrompts = new ArrayList<>();
    this.additionalPrompts.add(templateRenderer.getPrompt(PromptType.USER_PROMPT_JOB_SERIES_1));
    this.additionalPrompts.add(templateRenderer.getPrompt(PromptType.USER_PROMPT_JOB_SERIES_2));
    this.additionalPrompts.add(templateRenderer.getPrompt(PromptType.USER_PROMPT_JOB_SERIES_3));
  }

  @Override
  protected AiClientResponse searchSync(T request) {
    String model = request.getEngineSelection().model();
    if (request.getUser().getCv() != null) {
      //Upload cv if needed
      userCvService.refreshUserCvIfNeeded(request.getUser(), request.getEngineSelection().type());
    }

    request.setStore(true);
    log.info("Searching jobs for user {} with model {} with prompt {}", request.getUser().getUsername(), model, StringUtils.abbreviate(request.getUserPrompt(), 50));
    AiClientResponse response = jobsClient.searchJobs(request);

    String prevId = response.getId();
    for (String prompt : additionalPrompts) {
      log.info("Searching jobs for user {} with model {} with prompt {}", request.getUser().getUsername(), model, StringUtils.abbreviate(prompt, 50));
      request.setUserPrompt(prompt);
      request.setPrevResponseId(prevId);
      AiClientResponse otherResponse = jobsClient.searchJobs(request);
      response.addAll(otherResponse);
      prevId = otherResponse.getId();
    }
    request.setStore(false);

    response.getJobs().forEach(job -> {
      job.setPromptId(request.getPromptId());
      job.setSource(model);
    });
    log.info("{} found {} url's and are going to be validated", model, response.getJobs().size());
    return response;
  }

}
