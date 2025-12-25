package com.jobshunter.service.clients.gpt;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.ApplicationProperties;
import com.jobshunter.ApplicationProperties.Gpt;
import com.jobshunter.ApplicationProperties.ModelSpecific;
import com.jobshunter.model.Job;
import com.jobshunter.model.GptJobSearchRequest;
import com.jobshunter.dto.gptResponse.GptCompletionResponse;
import com.jobshunter.dto.gptResponse.OutputItem;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.UrlExtractor;
import com.jobshunter.service.testdata.DummyEconomyGpt;
import com.jobshunter.service.testdata.DummyPremiumGpt;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.jsonwebtoken.lang.Collections;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@PackageExpected("com.jobshunter.service.clients.gpt")
public abstract sealed class AbstractGptApiClient
    permits EconomyGptJobSearchImpl, PremiumGptJobSearchImpl, DummyEconomyGpt, DummyPremiumGpt {

  protected final ApplicationProperties properties;

  @Getter
  private String jobsSystemPrompt;

  @Getter
  private Object outputSchema;

  private final UrlExtractor urlExtractor;

  protected AbstractGptApiClient(ApplicationProperties properties, UrlExtractor urlExtractor) {
    this.properties = properties;
    this.urlExtractor = urlExtractor;
  }

  public abstract ModelSpecific getConfig();

  @RateLimiter(name = "openaiLimiter")
  public abstract List<Job> searchWithModel(String systemPrompt, String userPrompt, ModelSpecific cfg, String fileId);

  @PostConstruct
  protected void init() {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    try (var inputStream = getClass().getClassLoader().getResourceAsStream(
        "prompts/" + getConfig().getSystemPromptFile())) {
      //noinspection DataFlowIssue
      jobsSystemPrompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot load system prompt file", e);
    }
    try (var inputStream = getClass().getClassLoader().getResourceAsStream("prompts/gptJobsJsonOutputSchema.txt")) {
      //noinspection DataFlowIssue
      String schemaJson = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      outputSchema = mapper.readValue(schemaJson, Object.class);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot load output schema file", e);
    }
  }

  public List<Job> searchJobs(GptJobSearchRequest request) {
    Gpt gpt = properties.getGpt();
    if (gpt.getApiKey() == null || gpt.getApiKey().isBlank()) {
      log.warn("ChatGPT job search enabled but CHATGPT_API_KEY missing.");
      return List.of();
    }
    return searchWithModel(jobsSystemPrompt, request.getPrompt().getPrompt(), getConfig(), request.getFileId());
  }

  protected List<Job> extractJobs(GptCompletionResponse response) {
    if (Collections.isEmpty(response.output())) {
      return List.of();
    }
    Optional<OutputItem> item = response.output().stream()
        .filter(p -> Objects.equals(p.type(), "message") && !p.content().isEmpty())
        .findAny();
    if (item.isPresent()) {
      final List<Job> jobs = new ArrayList<>();
      item.get().content().stream()
          .filter(c -> c.text().length() > 2)
          .filter(c -> Objects.equals("output_text", c.type()))
          .forEach(o -> jobs.addAll(urlExtractor.parseJobs(o.text())));
      return jobs;
    } else {
      return java.util.Collections.emptyList();
    }
  }

}
