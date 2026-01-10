package com.jobshunter.service.application.hunting;

import com.jobshunter.dto.AdditionalEffortRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.Job;
import com.jobshunter.model.PromptType;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.clients.AiJobsClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class AdditionalEffortJobHunting<T extends AdditionalEffortRequest> extends GenericJobHunting<T> {

  private final TemplateRenderer templateRenderer;

  public AdditionalEffortJobHunting(Executor executor,
      AiJobsClient<T, AiClientResponse> jobsClient,
      UserCvService userCvService,
      TemplateRenderer templateRenderer
  ) {
    super(executor, jobsClient, userCvService);
    this.templateRenderer = templateRenderer;
  }

  @Override
  protected AiClientResponse searchSync(T request) {
    String model = request.getEngineSelection().model();
    if (request.getUser().getCv() != null) {
      //Upload cv if needed
      userCvService.refreshUserCvIfNeeded(request.getUser(), request.getEngineSelection().type());
    }
    log.info("Searching jobs for user {} with model {} with prompt {}", request.getUser().getUsername(), model,
        StringUtils.abbreviate(request.getUserPrompt(), 50));
    AiClientResponse response = jobsClient.searchJobs(request);
    log.info("Found {} jobs for {}", response.getJobs().size(), request.getUser().getUsername());

    if (request.getStoreConversation()) {
      chainConversation(request, response, model);
    }

    response.getJobs().forEach(job -> {
      job.setPromptId(request.getPromptId());
      job.setSource(model);
    });
    log.info("{} found {} url's and are going to be validated", model, response.getJobs().size());
    return response;
  }

  private void chainConversation(T request, AiClientResponse response, String model) {
    request.setPreviousURL(response.getJobs().stream().map(Job::getUrl).toList());
    String prevId = response.getId();
    for (PromptType promptType : List.of(
        PromptType.USER_PROMPT_JOB_SERIES_1,
        PromptType.USER_PROMPT_JOB_SERIES_2,
        PromptType.USER_PROMPT_JOB_SERIES_3,
        PromptType.USER_PROMPT_JOB_SERIES_4
    )) {
      if (promptType == PromptType.USER_PROMPT_JOB_SERIES_4 && request.getPreviousURL().size() < 2) {
        break;
      }
      //This is mandatory, otherwise the AI model will reply nothing in conversation.
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      AiClientResponse anotherResponse = jobsClient.searchJobs(request);
      log.info("Found {} jobs for {}", anotherResponse.getJobs().size(), request.getUser().getUsername());
      response.addAll(anotherResponse);

      //This is mandatory, otherwise the AI model will reply nothing in conversation.
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      Map<String, Object> params = switch (promptType) {
        case USER_PROMPT_JOB_SERIES_1 -> Map.of("timestamp", Instant.now());
        case USER_PROMPT_JOB_SERIES_2 -> Map.of("invalid_urls", request.getPreviousURL(), "invalid_reasons", RandomInvalidReasons.pick());
        case USER_PROMPT_JOB_SERIES_3 -> Map.of("timestamp", Instant.now());
        case USER_PROMPT_JOB_SERIES_4 -> {
          int mid = request.getPreviousURL().size() / 2;
          List<String> firstHalf = request.getPreviousURL().subList(0, mid);
          List<String> secondHalf = request.getPreviousURL().subList(mid, request.getPreviousURL().size());
          yield Map.of("timestamp", Instant.now(), "valid_results_json", firstHalf, "invalid_urls", secondHalf);
        }
        default -> Map.of();
      };
      String prompt = templateRenderer.getPrompt(promptType, params);

      log.info("Searching jobs for user {} with model {} with prompt {}", request.getUser().getUsername(), model,
          StringUtils.abbreviate(prompt, 50));
      request.setUserPrompt(prompt);
      request.setPrevResponseId(prevId);
      AiClientResponse otherResponse = jobsClient.searchJobs(request);
      log.info("Found {} jobs for {}", otherResponse.getJobs().size(), request.getUser().getUsername());

      response.addAll(otherResponse);
      prevId = otherResponse.getId();
    }
  }

}
