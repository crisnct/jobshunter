package com.jobshunter.service.application;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.dto.exceptions.BusinessException;
import com.jobshunter.model.EngineSelection;
import com.jobshunter.model.EngineTier;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.hunting.GeminiJobHunting;
import com.jobshunter.service.application.hunting.GptJobHunting;
import com.jobshunter.service.application.hunting.JobHunting;
import com.jobshunter.service.application.hunting.SerpJobHunting;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import com.jobshunter.service.application.notifiers.WhatsappNotifierService;
import com.jobshunter.service.application.processors.JobRedirection;
import com.jobshunter.service.application.processors.JobScoring;
import com.jobshunter.service.application.processors.JobsValidator;
import com.jobshunter.service.application.processors.UploadedFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class JobHuntService {

  private final WhatsappNotifierService whatsappNotifierService;

  private final EmailNotifierService emailNotifierService;

  private final UserDataService userDataService;

  private final SerpJobHunting serpJobHunting;

  private final GptJobHunting gptJobHunting;

  private final GeminiJobHunting geminiJobHunting;

  private final JobScoring jobScoring;

  private final JobsValidator jobsValidator;

  private final JobRedirection jobRedirection;

  private final Executor miscExecutor;

  private final Executor geminiExecutor;

  private final ApplicationProperties properties;

  public JobHuntService(
      WhatsappNotifierService whatsappNotifierService,
      EmailNotifierService emailNotifierService,
      UserDataService userDataService,
      SerpJobHunting serpJobHunting,
      GptJobHunting gptJobHunting,
      GeminiJobHunting geminiJobHunting,
      JobScoring jobScoring,
      JobsValidator jobsValidator,
      JobRedirection jobRedirection,
      ApplicationProperties properties,
      @Qualifier("miscellaneousExecutor") Executor miscExecutor,
      @Qualifier("geminiSearchExecutor") Executor geminiExecutor
  ) {
    this.whatsappNotifierService = whatsappNotifierService;
    this.emailNotifierService = emailNotifierService;
    this.userDataService = userDataService;
    this.serpJobHunting = serpJobHunting;
    this.gptJobHunting = gptJobHunting;
    this.geminiJobHunting = geminiJobHunting;
    this.jobScoring = jobScoring;
    this.jobsValidator = jobsValidator;
    this.jobRedirection = jobRedirection;
    this.geminiExecutor = geminiExecutor;
    this.miscExecutor = miscExecutor;
    this.properties = properties;
  }

  public void scheduledRun() throws IOException {
    log.info("Starts scheduled job hunt...");
    for (var user : userDataService.getAllUsers()) {
      if (user.isNotifyWhatsapp() || user.isNotifyEmail()) {
        if (user.getTimeInterval() != null
            && user.getTimeInterval() > 0
            && user.getLastJobs() != null
            && user.getLastJobs().plusMinutes(user.getTimeInterval()).isBefore(LocalDateTime.now())) {
          log.info("Start searching jobs for {} ", user.getUsername());
          this.searchJobsForUser(
              new SearchJobOrder(
                  user,
                  List.of(new EngineSelection(EngineType.GPT, EngineTier.ECONOMY))
              )
          );
        }
      }
    }
    log.info("Stop scheduled job hunt.");
  }

  public JobHuntResponse searchJobsForUser(SearchJobOrder order) {
    UserEntity user = order.user();
    final List<String> existingURLs;
    boolean isEnableOneRealEngine = (properties.getGemini().isEnabled() || properties.getGpt().isEnabled() || properties.getSerpApi().isEnabled());
    if (isEnableOneRealEngine) {
      existingURLs = userDataService.getExistingJobUrlsForUser(user.getUsername());
    } else {
      existingURLs = new ArrayList<>();
    }

    String resumeFileIdCleanup = null;
    try {
      String resumeFileId = jobScoring.uploadUserCv(user.getCv());
      resumeFileIdCleanup = resumeFileId;

      CompletableFuture<List<Job>> serpJobs = this.searchJobsAsync(EngineType.SERP, serpJobHunting, order);
      CompletableFuture<List<Job>> gptJobs = this.searchJobsAsync(EngineType.GPT, gptJobHunting, order);
      CompletableFuture<List<Job>> geminiJobs = this.searchJobsAsync(EngineType.GEMINI, geminiJobHunting, order);

      // Process each source independently and apply pipeline immediately (non-blocking)
      CompletableFuture<List<CompletableFuture<JobContext>>> serpPipelinedJobs = serpJobs
          .thenApply(jobsList -> deduplicateByUrl(jobsList, existingURLs))
          .thenApply(jobsList -> jobsList.stream()
              .map(job -> applyJobPipeline(job, user, resumeFileId))
              .toList());

      CompletableFuture<List<CompletableFuture<JobContext>>> gptPipelinedJobs = gptJobs
          .thenApply(jobsList -> deduplicateByUrl(jobsList, existingURLs))
          .thenApply(jobsList -> jobsList.stream()
              .map(job -> applyJobPipeline(job, user, resumeFileId))
              .toList());

      CompletableFuture<List<CompletableFuture<JobContext>>> geminiPipelinedJobs = geminiJobs
          .thenApply(jobsList -> deduplicateByUrl(jobsList, existingURLs))
          .thenApply(jobsList -> jobsList.stream()
              .map(job -> applyJobPipeline(job, user, resumeFileId))
              .toList());

      // Combine all pipelines and remove duplicates between sources
      CompletableFuture<List<JobContext>> allJobs = combineAndDeduplicatePipelines(
          serpPipelinedJobs, gptPipelinedJobs, geminiPipelinedJobs, existingURLs);

      List<JobContext> result = allJobs.join();
      this.logResults(result, user.getUsername());

      List<Job> validatedJobs = result.stream()
          .filter(JobContext::isAccepted)
          .map(JobContext::getJob)
          .toList();

      JobHuntResponse jobHuntResponse = new JobHuntResponse(validatedJobs.stream()
          .sorted(Comparator.comparing(Job::getScore).reversed())
          .toList());

      List<Job> jobs = jobHuntResponse.jobsFound();
      if (!jobs.isEmpty()) {
        if (isEnableOneRealEngine) {
          Map<Long, List<Job>> jobsByPromptId = jobs.stream().collect(Collectors.groupingBy(Job::getPromptId));
          this.userDataService.saveJobsToDB(jobsByPromptId, user);
        }
        if (user.isNotifyWhatsapp()) {
          whatsappNotifierService.send(jobs, user);
        }
        if (user.isNotifyEmail()) {
          emailNotifierService.sendUsingTemplate(jobs, user);
        }
      }
      return jobHuntResponse;
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    } finally {
      if (resumeFileIdCleanup != null) {
        jobScoring.cleanup(resumeFileIdCleanup);
      }
    }
  }

  // Deduplicate jobs by URL within same source
  private List<Job> deduplicateByUrl(List<Job> jobs, List<String> existingURLs) {
    Set<String> seenUrls = new HashSet<>(existingURLs);
    return jobs.stream()
        .filter(job -> job != null && job.getUrl() != null)
        .filter(job -> seenUrls.add(job.getUrl()))
        .toList();
  }

  // Combine all pipelined jobs and remove duplicates between sources
  private CompletableFuture<List<JobContext>> combineAndDeduplicatePipelines(
      CompletableFuture<List<CompletableFuture<JobContext>>> serpPipelinedJobs,
      CompletableFuture<List<CompletableFuture<JobContext>>> gptPipelinedJobs,
      CompletableFuture<List<CompletableFuture<JobContext>>> geminiPipelinedJobs,
      List<String> existingURLs) {

    return CompletableFuture.allOf(serpPipelinedJobs, gptPipelinedJobs, geminiPipelinedJobs)
        .thenCompose(v -> {
          List<CompletableFuture<JobContext>> allFutures = new ArrayList<>();
          allFutures.addAll(serpPipelinedJobs.join());
          allFutures.addAll(gptPipelinedJobs.join());
          allFutures.addAll(geminiPipelinedJobs.join());

          return CompletableFuture.allOf(allFutures.toArray(CompletableFuture[]::new))
              .thenApply(v2 -> allFutures.stream()
                  .map(CompletableFuture::join)
                  .toList())
              .thenApply(jobContexts -> removeDuplicatesBetweenSources(jobContexts, existingURLs));
        });
  }

  // Remove duplicates between sources (SERP vs GPT vs Gemini)
  private List<JobContext> removeDuplicatesBetweenSources(
      List<JobContext> jobContexts,
      List<String> existingURLs) {
    Set<String> seenUrls = new HashSet<>(existingURLs);

    return jobContexts.stream()
        .filter(jc -> {
          String url = jc.getJob().getUrl();
          return url != null && seenUrls.add(url);
        })
        .toList();
  }

  private void logResults(List<JobContext> result, String username) {
    long acceptedUrls = result.stream().filter(JobContext::isAccepted).count();
    long rejected = result.size() - acceptedUrls;
    long errors = result.stream().filter(JobContext::isFailed).count();

    StringBuilder validLinksBuilder = new StringBuilder();
    if (acceptedUrls > 0) {
      result.stream().filter(JobContext::isAccepted).forEach(jc -> {
        if (!validLinksBuilder.isEmpty()) {
          validLinksBuilder.append("\n");
        }
        validLinksBuilder.append(jc.getJob().getSource());
        validLinksBuilder.append(": ");
        validLinksBuilder.append(jc.getJob().getUrl());
      });
    }
    StringBuilder errorBuilder = new StringBuilder();
    if (errors > 0) {
      result.stream().filter(JobContext::isFailed).forEach(jc -> {
        if (!errorBuilder.isEmpty()) {
          errorBuilder.append("\n");
        }
        errorBuilder.append(jc.getFailureMessage());
      });
    }
    result.stream().filter(jc -> jc.getPhase().ordinal() < JobPhase.SCORED.ordinal()).forEach(jc -> {
      errorBuilder.append("\nWrong phase: ");
      errorBuilder.append(jc.getPhase().name());
      errorBuilder.append(" ");
      errorBuilder.append(jc.getJob().getPromptId());
      errorBuilder.append(" ");
      errorBuilder.append(jc.getFailureMessage());
    });

    log.info("""
        \n---Job search results for {} -----------------------------------------
        Total jobs found: {}
        Valid links: {}
        Rejected: {}
        Errors: {}
        
        Valid links:
        {}
        
        Errors:
        {}
        """, username, result.size(), acceptedUrls, rejected, errors, validLinksBuilder, errorBuilder
    );
  }

  private CompletableFuture<List<JobContext>> flattenAndWaitForAllFutures(
      CompletableFuture<List<CompletableFuture<JobContext>>> serpPipelinedJobs,
      CompletableFuture<List<CompletableFuture<JobContext>>> gptPipelineJobs,
      CompletableFuture<List<CompletableFuture<JobContext>>> geminiPipelineJobs) {

    List<CompletableFuture<JobContext>> allFutures = collectAllFutures(
        serpPipelinedJobs, gptPipelineJobs, geminiPipelineJobs);

    return CompletableFuture.allOf(allFutures.toArray(CompletableFuture[]::new))
        .thenApply(v -> joinAllFutures(allFutures));
  }

  private List<CompletableFuture<JobContext>> collectAllFutures(
      CompletableFuture<List<CompletableFuture<JobContext>>>... pipelinedJobs) {

    return Stream.of(pipelinedJobs)
        .flatMap(f -> f.join().stream())
        .toList();
  }

  private List<JobContext> joinAllFutures(List<CompletableFuture<JobContext>> futures) {
    return futures.stream()
        .map(CompletableFuture::join)
        .toList();
  }

  private CompletableFuture<JobContext> applyJobPipeline(Job job, UserEntity user, String resumeFileId) {
    return CompletableFuture.supplyAsync(() -> new JobContext(job, user, resumeFileId), miscExecutor)
        .thenApplyAsync(jobRedirection::processAsync, miscExecutor)
        .thenApplyAsync(jobsValidator::processAsync, miscExecutor)
        .thenApplyAsync(jobScoring::processAsync, geminiExecutor)
        .handleAsync((jc, ex) -> {
          if (ex != null) {
            log.error("Pipeline failed for job {}: {}", job != null ? job.getUrl() : "unknown", ex.getMessage(), ex);
            return JobContext.failed(job, user, resumeFileId, ex);
          }
          if (jc == null) {
            log.error("Pipeline returned null context for job {}", job != null ? job.getUrl() : "unknown");
            return JobContext.failed(job, user, resumeFileId, new IllegalStateException("Pipeline returned null JobContext"));
          }
          return jc;
        }, miscExecutor);
  }

  private CompletableFuture<List<Job>> searchJobsAsync(
      EngineType engineType,
      JobHunting jobHunting,
      SearchJobOrder order
  ) {
    List<EngineSelection> enginesFiltered = order.engines().stream()
        .filter(selection -> selection.type() == engineType)
        .toList();
    if (enginesFiltered.isEmpty()) {
      return CompletableFuture.completedFuture(List.of());
    } else {
      return jobHunting.searchJobsAsync(new SearchJobOrder(order.user(), enginesFiltered));
    }

  }

}
