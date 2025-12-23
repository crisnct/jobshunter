package com.jobshunter.service.clients.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.config.ApplicationProperties.ModelSpecific;
import com.jobshunter.dto.Job;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse;
import com.jobshunter.dto.geminiResponse.GeminiGenerateContentResponse.Candidate;
import com.jobshunter.dto.geminiRequest.Part;
import com.jobshunter.dto.gptResponse.JobResults;
import com.jobshunter.processor.PackageExpected;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

@Slf4j
@PackageExpected("com.jobshunter.service.clients.gpt")
public abstract class AbstractGeminiApiClient {

  private JsonMapper mapper;

  @Getter
  private String jobsSystemPrompt;

  @Getter
  private Object outputSchema;

  public abstract ModelSpecific getConfig();

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
    try (var inputStream = getClass().getClassLoader().getResourceAsStream("prompts/geminiJobsJsonOutputSchema.txt")) {
      //noinspection DataFlowIssue
      String schemaJson = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      outputSchema = mapper.readValue(schemaJson, Object.class);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot load output schema file", e);
    }
  }

  protected List<Job> extractJobs(GeminiGenerateContentResponse response) {
    if (response == null || response.candidates() == null) {
      return List.of();
    }
    return response.candidates().stream()
        .map(Candidate::content)
        .filter(c -> c != null && c.parts() != null)
        .flatMap(c -> c.parts().stream())
        .map(Part::text)
        .filter(text -> text != null && text.length() > 2)
        .flatMap((Function<String, Stream<Job>>) s -> parseJobs(s).stream())
        .toList();
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
      log.error("Failed to parse jobs from Gemini response: {}", e.getMessage());
      return List.of();
    }
  }

}
