package com.jobshunter.service.clients.gpt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.config.ApplicationProperties.Gpt;
import com.jobshunter.dto.Job;
import com.jobshunter.processor.PackageExpected;
import io.jsonwebtoken.lang.Collections;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

@Slf4j
@PackageExpected("com.jobshunter.service.application")
public abstract sealed class AbstractGptApiClient<T extends Gpt>
    implements GPTSearchApiClient
    permits GptApi4Client, GptApi5Client {

  private JsonMapper mapper;

  @Getter
  private String jobsSystemPrompt;

  public abstract T getConfig();

  public abstract List<Job> searchWithModel(String systemPrompt, String userPrompt, T cfg, String fileId);

  @PostConstruct
  protected void init() throws IOException {
    mapper = JsonMapper.builder().findAndAddModules().build();
    try (var inputStream = getClass().getClassLoader().getResourceAsStream(
        "prompts/" + getConfig().getSystemPromptFile())) {
      //noinspection DataFlowIssue
      jobsSystemPrompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot load system prompt file", e);
    }
  }

  public final List<Job> search(String userPrompt, String fileId) {
    T cfg = getConfig();
    if (cfg == null) {
      return List.of();
    }
    if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
      log.warn("ChatGPT job search enabled but CHATGPT_API_KEY missing.");
      return List.of();
    }
    return searchWithModel(jobsSystemPrompt, userPrompt, cfg, fileId);
  }

  protected List<Job> extractJobs(GptCompletionResponse response) {
    if (Collections.isEmpty(response.output())) {
      return List.of();
    }
    Optional<OutputItem> item = response.output().stream()
        .filter(p -> Objects.equals(p.type, "message") && !p.content().isEmpty())
        .findAny();
    if (item.isPresent()) {
      final List<Job> jobs = new ArrayList<>();
      item.get().content.stream()
          .filter(c -> c.text.length() > 2)
          .filter(c -> Objects.equals("output_text", c.type))
          .forEach(o -> jobs.addAll(parseJobs(o.text)));
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
      Job[] parsed = mapper.readValue(text, Job[].class);
      return Arrays.stream(parsed).map(job -> new Job(job.score(), job.url(), getConfig().getModel().toUpperCase())).toList();
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse jobs from ChatGPT response: {}", e.getMessage());
      return List.of();
    }
  }


  @JsonIgnoreProperties(ignoreUnknown = true)
  protected record GptCompletionResponse(List<OutputItem> output) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record OutputItem(String id, String type, String status, List<ContentItem> content) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ContentItem(String type, String text) {

  }

}
