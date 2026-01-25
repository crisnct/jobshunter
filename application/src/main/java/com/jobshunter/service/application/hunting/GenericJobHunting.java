package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobRoleEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.AiJobsCompaniesClient;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract non-sealed class GenericJobHunting implements JobHunting {

  protected final AiJobsClient jobsClient;
  protected final UserCvService userCvService;
  private final Executor executor;
  private final CountryIsoCode countryIsoCode;

  public GenericJobHunting(
      Executor executor,
      AiJobsClient jobsClient,
      CountryIsoCode countryIsoCode,
      UserCvService userCvService
  ) {
    this.executor = executor;
    this.countryIsoCode = countryIsoCode;
    this.jobsClient = jobsClient;
    this.userCvService = userCvService;
  }

  public abstract EngineType getEngineType();

  public AIJobSearchRequest createRequest(SearchJobOrder order) {
    AIJobSearchRequest request = new AIJobSearchRequest(order);
    order.getUser().getRemoteCvs().stream()
        .filter(p -> p.getProvider() == getEngineType()).findAny()
        .ifPresent(userRemoteCvEntity -> request.setFileId(userRemoteCvEntity.getFileId()));
    request.setBase64CV(Base64.getEncoder().encodeToString(order.getUser().getCv().getByteArray()));
    request.setStoreConversation(true);
    if (countryIsoCode != null) {
      request.setCountryIsoCode(countryIsoCode.getCode(order.getUser().getCountry()));
    }
    return request;
  }

  public AIJobSearchRequest createRequest(SearchJobOrder order, String prompt, Long promptId) {
    AIJobSearchRequest request = this.createRequest(order);
    request.setUserPrompt(prompt);
    request.setPromptId(promptId);
    return request;
  }

  public AIJobSearchRequest createRequest(@NotNull SearchJobOrder order, @NotNull UserPromptEntity prompt) {
    return createRequest(order, prompt.getPrompt(), prompt.getId());
  }

  @Override
  public CompletableFuture<List<Job>> searchJobsAsync(SearchJobOrder order) {
    //Search companies and then for each company search jobs
    CompletableFuture<List<Job>> futures = CompletableFuture.completedFuture(List.of());

    if (order.getModel().getProvider() == EngineType.SERP) {
      for (UserJobRoleEntity role : order.getUser().getJobRoles()) {
        AIJobSearchRequest request = createRequest(order, role.getJobRole(), null);
        CompletableFuture<AiClientResponse> jobsFound = this.searchAsync(request, executor);
        futures = futures.thenCombine(jobsFound, (previousJobs, newJobs) -> {
          List<Job> merged = new ArrayList<>(previousJobs);
          merged.addAll(newJobs.getJobs());
          return merged;
        });
      }
    } else {
      //Search jobs based on user requests
      for (UserPromptEntity prompt : order.getUser().getPrompts()) {
        AIJobSearchRequest request = createRequest(order, prompt);
        CompletableFuture<AiClientResponse> jobsFound = this.searchAsync(request, executor);
        futures = futures.thenCombine(jobsFound, (previousJobs, newJobs) -> {
          List<Job> merged = new ArrayList<>(previousJobs);
          merged.addAll(newJobs.getJobs());
          return merged;
        });
      }
    }

    return futures;
  }

  public CompletableFuture<List<Job>> searchJobsByCompaniesAsync(SearchJobOrder order) {
    UserEntity user = order.getUser();
    AIJobSearchRequest request = createRequest(order);
    //TODO optimize and create an async for each of the job search for a group of companies.
    // Do it similar like in searchJobsAsync so it will run all jobs search in parallel
    if (jobsClient instanceof AiJobsCompaniesClient jobsClientComp) {
      return CompletableFuture.supplyAsync(() -> searchCompaniesSync(request, order.getModel(), jobsClientComp), executor)
          .exceptionally(throwable -> {
            if (throwable.getCause() != null && throwable.getCause() instanceof RequestNotPermitted) {
              log.error("❌ Rate limit exceeded for user {} model {}", user.getUsername(), order.getModel());
            } else {
              if (throwable.getCause() != null) {
                log.error("Unexpected error at gathering jobs from model {}: {}", order.getModel(),
                    throwable.getCause().getMessage());
              } else {
                log.error("Unexpected error at gathering jobs from model {}: {}", order.getModel(), throwable.getMessage());
              }
            }
            return List.of();
          });
    } else {
      log.warn("No implemented company search for {}", order.getModel().getModel());
      return CompletableFuture.completedFuture(List.of());
    }
  }

  protected CompletableFuture<AiClientResponse> searchAsync(AIJobSearchRequest request, Executor executor) {
    return CompletableFuture.supplyAsync(() -> searchSync(request), executor)
        .exceptionally(throwable -> {
          AiModelEntity aiModel = request.getOrder().getModel();
          if (throwable.getCause() != null && throwable.getCause() instanceof RequestNotPermitted) {
            log.error("❌ Rate limit exceeded for user {}, engine: {}, eodel: {}",
                request.getOrder().getUser().getUsername(), aiModel.getProvider(), aiModel.getModel());
          } else {
            log.error("Unexpected error at gathering jobs from model {}: {} for prompt {}", aiModel.getModel(),
                throwable.getMessage(), request.getUserPrompt());
          }
          return new AiClientResponse();
        });
  }

  protected AiClientResponse searchSync(AIJobSearchRequest request) {
    AiModelEntity aiModel = request.getOrder().getModel();
    UserEntity user = request.getOrder().getUser();
    log.info("Searching jobs for user {} with model {}", user.getUsername(), aiModel.getModel());
    AiClientResponse response = jobsClient.searchJobs(request);
    response.getJobs().forEach(job -> {
      job.setPromptId(request.getPromptId());
      job.setSource(aiModel.getModel());
    });
    log.info("{} found {} url's and are going to be validated", aiModel.getModel(), response.getJobs().size());
    return response;
  }

  private List<Job> searchCompaniesSync(AIJobSearchRequest request, AiModelEntity model, AiJobsCompaniesClient aiJobCompany) {
    UserEntity user = request.getOrder().getUser();
    log.info("Searching companies for user {} with model {}", user.getUsername(), model.getModel());
    List<CompanyDto> companies = aiJobCompany.searchCompanies(request);
    log.info("Found {} companies for user {} with model {}, searching jobs now...", companies.size(), user.getUsername(),
        model.getModel());

    int counter = 0;
    List<Job> jobsFound = new ArrayList<>();
    for (CompanyDto company : companies) {
      log.info("{}/{} Searching jobs for user {} from company: {} with model {}",
          ++counter, companies.size(), user.getUsername(), company.companyName(), request.getDiscoveryModel().getModel());
      request.setCompany(company);
      List<Job> jobs = aiJobCompany.searchJobsFromCompanies(request).getJobs();
      jobsFound.addAll(jobs);
      log.info("Found {} jobs for user {} from company {}", jobs.size(), user.getUsername(), company.companyName());
    }
    jobsFound.forEach(job -> job.setSource("COMP-" + model.getModel()));
    return jobsFound;
  }

}
