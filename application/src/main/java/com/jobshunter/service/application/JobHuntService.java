package com.jobshunter.service.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.EngineType;
import com.jobshunter.dto.Job;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.dto.SearchJobOrder;
import com.jobshunter.dto.gemini.GeminiJobSearchRequest;
import com.jobshunter.dto.gptRequest.GptJobSearchRequest;
import com.jobshunter.dto.serpRequest.SearchWithSerpRequest;
import com.jobshunter.dto.serpResponse.SerpApiJobHit;
import com.jobshunter.dto.serpResponse.SerpApiJobsResult;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import com.jobshunter.service.application.notifiers.WhatsappNotifierService;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.GptJobScoreCalculatorClient;
import com.jobshunter.service.clients.SerpApiClient;
import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class JobHuntService {

  @Autowired
  private ApplicationProperties properties;

  @Autowired
  @Qualifier("EconomyJobsClientGPT")
  private AiJobsClient<GptJobSearchRequest, List<Job>> gptEconomy;

  @Autowired
  @Qualifier("PremiumJobsClientGPT")
  private AiJobsClient<GptJobSearchRequest, List<Job>> gptPremium;

  @Autowired
  @Qualifier("EconomyJobsClientGemini")
  private AiJobsClient<GeminiJobSearchRequest, List<Job>> geminiEconomy;

  @Autowired
  private GptJobScoreCalculatorClient scoreCalculator;

  @Autowired
  private SerpApiClient<SearchWithSerpRequest, SerpApiJobsResult> serpApiClient;

  @Autowired
  @Qualifier("gptSearchExecutor")
  private Executor gptSearchExecutor;

  @Autowired
  @Qualifier("geminiSearchExecutor")
  private Executor geminiSearchExecutor;

  @Autowired
  private WhatsappNotifierService whatsappNotifierService;

  @Autowired
  private EmailNotifierService emailNotifierService;

  @Autowired
  private UserDataService userDataService;

  private List<Pattern> expiredJobsPatterns;

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private JsonMapper mapper;

  @PostConstruct
  public void init() {
    expiredJobsPatterns = new ArrayList<>();
    for (String keyword : properties.getJobsHunter().getExpiredExpressions().split(",")) {
      expiredJobsPatterns.add(Pattern.compile(">[^<]{0,500}" + Pattern.quote(keyword) + "[^<]{0,500}<",
          Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
    }
    expiredJobsPatterns = Collections.unmodifiableList(expiredJobsPatterns);
  }

  public void scheduledRun() {
    log.info("Starts scheduled job hunt...");
    for (var user : userDataService.getAllUsers()) {
      if (user.isNotifyWhatsapp() || user.isNotifyEmail()) {
        if (user.getTimeInterval() != null
            && user.getTimeInterval() > 0
            && user.getLastJobs() != null
            && user.getLastJobs().plusMinutes(user.getTimeInterval()).isBefore(LocalDateTime.now())) {
          log.info("Start searching jobs for {} ", user.getUsername());
          this.searchJobsForUser(new SearchJobOrder(
              user,
              List.of(EngineType.GPT4),
              properties.getJobsHunter().getIterationPerUser()
          ));
        }
      }
    }
    log.info("Stop scheduled job hunt.");
  }

  public JobHuntResponse searchJobsForUser(SearchJobOrder order) {
    UserEntity user = order.user();
    String gptFileId = user.getCv() != null ? user.getCv().getGptFileId() : null;
    if (user.getPrompts().isEmpty() || !StringUtils.hasText(gptFileId)) {
      log.info("Skip user {} because prompt or cv is missing", user.getUsername());
      return new JobHuntResponse(Collections.emptyList());
    }

    final JobsSynchronizer jobsSync =
        new JobsSynchronizer(userDataService.getExistingJobUrlsForUser(user.getUsername()), this::isValidJob);

    List<CompletableFuture<Void>> enginesFutures = new ArrayList<>();
    if (order.engines().stream().anyMatch(p -> p == EngineType.SERP)) {
      enginesFutures.add(CompletableFuture.runAsync(() -> searchWithSerpAPi(jobsSync, user, gptFileId)));
    }

    List<EngineType> gptEngines = order.engines().stream().filter(p -> p.name().startsWith("GPT")).toList();
    if (!gptEngines.isEmpty()) {
      SearchJobOrder orderClone = new SearchJobOrder(
          order.user(),
          gptEngines,
          order.iterations()
      );
      enginesFutures.add(gptSearch(jobsSync, orderClone, gptFileId));
    }

    List<EngineType> geminiEngines = order.engines().stream().filter(p -> p.name().startsWith("GEMINI")).toList();
    if (!geminiEngines.isEmpty()) {
      SearchJobOrder orderClone = new SearchJobOrder(
          order.user(),
          geminiEngines,
          order.iterations()
      );
      enginesFutures.add(geminiSearch(jobsSync, orderClone));
    }

    CompletableFuture.allOf(enginesFutures.toArray(CompletableFuture[]::new)).join();

    JobHuntResponse jobHuntResponse = new JobHuntResponse(jobsSync.getJobs().stream()
        .sorted(Comparator.comparing(Job::score).reversed())
        .toList());

    List<Job> jobs = jobHuntResponse.jobsFound();
    if (jobs.isEmpty()) {
      log.info("No jobs found for user {} ", user.getUsername());
    } else if (user.isNotifyEmail() || user.isNotifyWhatsapp()) {
      userDataService.updateUser(user, jobs);
      log.info("Found {} jobs for {} ", jobs.size(), user.getEmail());
      jobs.forEach(System.out::println);

      if (user.isNotifyWhatsapp()) {
        whatsappNotifierService.send(jobs, user);
      }
      if (user.isNotifyEmail()) {
        emailNotifierService.sendUsingTemplate(jobs, user);
      }
    }

    return jobHuntResponse;
  }

  private CompletableFuture<Void> gptSearch(JobsSynchronizer jobsSync, SearchJobOrder order, String gptFileId) {
    UserEntity user = order.user();
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    int delayCounter = 0;
    for (int i = 0; i < order.iterations(); i++) {
      for (EngineType engine : order.engines()) {
        for (UserPromptEntity prompt : user.getPrompts()) {
          if (prompt.getEngine() == engine) {
            Executor delayedExecutor = CompletableFuture.delayedExecutor(
                (long) delayCounter++ * order.iterations(),
                TimeUnit.MILLISECONDS,
                gptSearchExecutor
            );
            GptJobSearchRequest request = new GptJobSearchRequest(user.getUsername(), prompt, engine, gptFileId);
            futures.add(CompletableFuture.runAsync(() -> gptSearch(jobsSync, request), delayedExecutor));
          }
        }
      }
    }

    // Combine all async iteration futures into one
    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
        .exceptionally(ex -> {
          log.error("GPT search failed for {}", user.getUsername(), ex);
          return null;
        });
  }

  private CompletableFuture<Void> geminiSearch(JobsSynchronizer jobsSync, SearchJobOrder order) {
    UserEntity user = order.user();
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    String userCVBase64 = Base64.getEncoder().encodeToString(user.getCv().getCv());
    int delayCounter = 0;
    for (int i = 0; i < order.iterations(); i++) {
      for (EngineType engine : order.engines()) {
        for (UserPromptEntity prompt : user.getPrompts()) {
          if (prompt.getEngine() == engine) {
            Executor delayedExecutor = CompletableFuture.delayedExecutor(
                (long) delayCounter++ * order.iterations(),
                TimeUnit.MILLISECONDS,
                geminiSearchExecutor
            );
            GeminiJobSearchRequest request = new GeminiJobSearchRequest(user.getUsername(), prompt, userCVBase64, engine);
            futures.add(CompletableFuture.runAsync(() -> geminiSearch(jobsSync, request), delayedExecutor));
          }
        }
      }
    }

    // Combine all async iteration futures into one
    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
        .exceptionally(ex -> {
          log.error("GPT search failed for {}", user.getUsername(), ex);
          return null;
        });
  }

  private void gptSearch(
      JobsSynchronizer jobsSync,
      GptJobSearchRequest request
  ) {
    log.info("Searching jobs for user {} with gpt model {}", request.getUsername(), request.getEngine());
    List<Job> jobsFound = switch (request.getEngine()) {
      case GPT4 -> gptEconomy.searchJobs(request);
      case GPT5 -> gptPremium.searchJobs(request);
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GPT model");
    };
    jobsSync.addJobs(jobsFound);
    userDataService.incrementPromptJobsFound(request.getPrompt().getId(), jobsFound.size());
    log.info("Found {} jobs for {}. Are going to be validated.", jobsFound.size(), request.getUsername());
  }

  private void geminiSearch(
      JobsSynchronizer jobsSync,
      GeminiJobSearchRequest request
  ) {
    log.info("Searching jobs for user {} with gpt model {}", request.getUsername(), request.getEngine());
    List<Job> jobsFound = switch (request.getEngine()) {
      case GEMINI_2_5_FLASH -> throw new IllegalStateException("Not implemented yet");
      case GEMINI_2_5_FLASH_LITE -> geminiEconomy.searchJobs(request);
      case GEMINI_2_5_PRO -> throw new IllegalStateException("Not implemented yet");
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GPT model");
    };
    jobsSync.addJobs(jobsFound);
    userDataService.incrementPromptJobsFound(request.getPrompt().getId(), jobsFound.size());
    log.info("{} found {} jobs for {}. Are going to be validated.", request.getEngine().name(), jobsFound.size(), request.getUsername());
  }

  private void searchWithSerpAPi(JobsSynchronizer jobsSync, UserEntity user, String gptFileId) {
    try {
      log.info("Searching jobs for user {} with serp api", user.getUsername());
      String serpPayload = user.getPrompts().stream()
          .filter(p -> p.getEngine() == EngineType.SERP)
          .map(UserPromptEntity::getPrompt)
          .findFirst()
          .orElse(null);
      if (Strings.isEmpty(serpPayload)) {
        log.info("Skip serp api search for {} because serp prompt is missing", user.getUsername());
        return;
      }
      SearchWithSerpRequest request = mapper.readValue(serpPayload, SearchWithSerpRequest.class);
      SerpApiJobsResult serpApiResult = serpApiClient.searchJobs(request);

      for (SerpApiJobHit job : serpApiResult.jobs()) {
        String jobDescription = job.description() + "\n" + job.highlights();
        int score = scoreCalculator.computeScore(jobDescription, gptFileId);
        jobsSync.addJob(new Job(score, job.applyLinks().getFirst(), "Google"));
      }
      log.info("Serp Api found {} jobs for user {}", serpApiResult.jobs().size(), user.getUsername());
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
  }

  private boolean isValidJob(String jobURL) {
    URI uri = toSafeHttpUri(jobURL);
    if (uri == null) {
      log.warn("Skipping URL {} because it is not a permitted HTTP/HTTPS target", jobURL);
      return false;
    }
    log.info("Testing URL {} ", uri);
    HttpHeaders headers = new HttpHeaders();
    headers.set("User-Agent", "Mozilla/5.0");
    headers.setAccept(List.of(MediaType.TEXT_HTML));
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    try {
      ResponseEntity<String> response = restTemplate.exchange(
          uri,
          HttpMethod.GET,
          entity,
          String.class
      );

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        String html = Jsoup.parse(response.getBody()).text().toLowerCase();
        boolean isExpired = expiredJobsPatterns.stream().anyMatch(pattern -> pattern.matcher(html).find());
        log.info("Expired: {} {}", isExpired, jobURL);
        return !isExpired;
      } else {
        log.info("Expired: true {}", jobURL);
        return false;
      }
    } catch (Exception e) {
      log.info("Expired: true {} {}", jobURL, e.getMessage());
      return false;
    }
  }

  private URI toSafeHttpUri(String jobURL) {
    if (jobURL == null || jobURL.isBlank()) {
      return null;
    }
    try {
      URI uri = URI.create(jobURL.trim());
      String scheme = uri.getScheme();
      if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
        return null;
      }
      String host = uri.getHost();
      if (host == null || host.isBlank()) {
        return null;
      }
      InetAddress address = InetAddress.getByName(host);
      if (address.isAnyLocalAddress()
          || address.isLoopbackAddress()
          || address.isLinkLocalAddress()
          || address.isSiteLocalAddress()
          || address.isMulticastAddress()) {
        return null;
      }
      return uri;
    } catch (Exception ex) {
      return null;
    }
  }

}
