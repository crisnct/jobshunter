package com.jobshunter.service.clients.gpt;

import com.jobshunter.config.ApplicationProperties.ChatGpt;
import com.jobshunter.dto.Job;
import com.jobshunter.processor.PackageExpected;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@PackageExpected("com.jobshunter.service.application")
public abstract sealed class AbstractGptApiClient<T extends ChatGpt>
    implements GPTSearchApiClient
    permits ChatGptApi4Client, ChatGptApi5Client {

  private String systemPrompt;

  public abstract T getConfig();

  public abstract List<Job> searchWithModel(String systemPrompt, String userPrompt, T cfg, String fileId);

  @PostConstruct
  private void init() {
    try (var inputStream = getClass().getClassLoader().getResourceAsStream(
        "systemPrompts/" + getConfig().getSystemPromptFile())) {
      if (inputStream == null) {
        throw new IllegalStateException("systemPromptFile is not specified in application.yml file");
      }
      systemPrompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
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
    return searchWithModel(systemPrompt, userPrompt, cfg, fileId);
  }

}
