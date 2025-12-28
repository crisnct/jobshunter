package com.jobshunter.service.clients.gpt;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.ApplicationProperties;
import com.jobshunter.dto.gptResponse.GptCompletionResponse;
import com.jobshunter.dto.gptResponse.OutputItem;
import com.jobshunter.model.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.application.UrlExtractor;
import com.jobshunter.service.testdata.FakeGptEconomy;
import com.jobshunter.service.testdata.FakeGptPremium;
import io.jsonwebtoken.lang.Collections;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@PackageExpected("com.jobshunter.service.clients.gpt")
@RequiredArgsConstructor
public abstract sealed class AbstractGptApiClient
    permits EconomyGptJobSearchImpl, PremiumGptJobSearchImpl, FakeGptEconomy, FakeGptPremium {

  protected final ApplicationProperties properties;

  @Getter
  private String jobsSystemPrompt;

  @Getter
  private Object outputSchema;

  private final UrlExtractor urlExtractor;

  public abstract String getSystemPromptFilename();

  @PostConstruct
  protected void init() {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    try (var inputStream = getClass().getClassLoader().getResourceAsStream(
        "prompts/" + getSystemPromptFilename())) {
      //noinspection DataFlowIssue
      jobsSystemPrompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Cannot load system prompt file", e);
    }
    try (var inputStream = getClass().getClassLoader().getResourceAsStream("prompts/gptJobsJsonOutputSchema.txt")) {
      //noinspection DataFlowIssue
      String schemaJson = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      outputSchema = mapper.readValue(schemaJson, Object.class);
    } catch (Exception e) {
      throw new RuntimeException("Cannot load output schema file", e);
    }
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
