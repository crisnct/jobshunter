package com.jobshunter.service.clients.gpt;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.config.ApplicationProperties.ChatGpt5;
import com.jobshunter.dto.Job;
import com.jobshunter.processor.PackageExpected;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@PackageExpected("com.jobshunter.service.application")
public non-sealed class ChatGptApi5Client extends AbstractGptApiClient<ChatGpt5> {

  private static final URI DEFAULT_URI = URI.create("https://api.openai.com/v1/responses");

  @Autowired
  private RestClient restClient;

  @Autowired
  private ApplicationProperties properties;

  @Override
  public ChatGpt5 getConfig() {
    return properties.getChatgpt5();
  }

  @Override
  public List<Job> searchWithModel(String systemPrompt, String userPrompt, ChatGpt5 cfg, String fileId) {
    try {
      ChatGpt5Payload payload = new ChatGpt5Payload(
          cfg.getModel(),
          cfg.getTemperature(),
          cfg.getMaxTokens(),
          List.of(new Tools(cfg.getToolsType())),
          List.of(
              new Input("system", List.of(new InputMessage("input_text", systemPrompt))),
              new Input("user", List.of(
                  new InputMessage("input_text", userPrompt),
                  new InputFile(fileId)
              ))
          )
      );

      ChatCompletionResponse response = restClient.post()
          .uri(DEFAULT_URI)
          .header("Authorization", "Bearer " + cfg.getApiKey())
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(ChatCompletionResponse.class);

      //noinspection DataFlowIssue
      return extractJobs(response);
    } catch (Exception e) {
      log.error("ChatGPT job API call failed: {}", e.getMessage());
      return List.of();
    }
  }


  private record ChatGpt5Payload(
      String model,
      double temperature,
      int max_output_tokens,
      List<Tools> tools,
      List<Input> input
  ) {

  }

  private sealed interface InputObj permits InputMessage, InputFile {

  }

  private record Input(String role, List<InputObj> content) {

  }

  private record InputMessage(String type, String text) implements InputObj {

  }

  private record InputFile(String type, String file_id) implements InputObj {

    public InputFile(String file_id) {
      this("input_file", file_id);
    }
  }

  private record Tools(String type) {

  }


}
