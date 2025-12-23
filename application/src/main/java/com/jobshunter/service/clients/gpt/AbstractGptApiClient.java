package com.jobshunter.service.clients.gpt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.config.ApplicationProperties.Gpt;
import com.jobshunter.config.ApplicationProperties.ModelSpecific;
import com.jobshunter.dto.Job;
import com.jobshunter.dto.gptRequest.GptJobSearchRequest;
import com.jobshunter.dto.gptResponse.GptCompletionResponse;
import com.jobshunter.dto.gptResponse.JobResults;
import com.jobshunter.dto.gptResponse.OutputItem;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.testdata.DummyEconomyGpt;
import com.jobshunter.testdata.DummyPremiumGpt;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.jsonwebtoken.lang.Collections;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@PackageExpected("com.jobshunter.service.clients.gpt")
public abstract sealed class AbstractGptApiClient

    permits EconomyGptJobSearchImpl, PremiumGptJobSearchImpl, DummyEconomyGpt, DummyPremiumGpt {

  private JsonMapper mapper;

  @Autowired
  private ApplicationProperties properties;

  @Getter
  private String jobsSystemPrompt;

  @Getter
  private Object outputSchema;

  public abstract ModelSpecific getConfig();

  @RateLimiter(name = "openaiLimiter")
  public abstract List<Job> searchWithModel(String systemPrompt, String userPrompt, ModelSpecific cfg, String fileId);

  @PostConstruct
  protected void init() {
    mapper = JsonMapper.builder().findAndAddModules().build();
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
          .forEach(o -> jobs.addAll(parseJobs(o.text())));
      return jobs;
    } else {
      return java.util.Collections.emptyList();
    }
  }

  private List<Job> parseJobs(String text) {
    if (Strings.isBlank(text)) {
      return List.of();
    }
    try {
      text = text.replaceFirst("```json", "");
      text = text.replace("```", "");
      JobResults parsed = mapper.readValue(text, JobResults.class);
      return Arrays.stream(parsed.results()).map(job -> new Job(job.score(), job.url(), getConfig().getModel())).toList();
    } catch (JsonProcessingException e) {
      log.error("Failed to parse jobs from ChatGPT response: {}", e.getMessage());
      return List.of();
    }
  }

}
