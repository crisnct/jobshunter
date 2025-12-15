package com.jobshunter.service.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class UserMessagesFactory {

  private static final String TEMPLATE_FOLDER = "messageTemplates/";

  private final Map<MessageTemplate, String> cache = new ConcurrentHashMap<>();

  public String build(MessageTemplate template, Map<String, String> placeholders) {
    String templateBody = cache.computeIfAbsent(template, this::readTemplate);
    return applyPlaceholders(templateBody, placeholders);
  }

  private String readTemplate(MessageTemplate template) {
    String location = TEMPLATE_FOLDER + template.fileName;
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(location)) {
      if (inputStream == null) {
        throw new IllegalStateException("Template file not found: " + location);
      }
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read template: " + location, e);
    }
  }

  private String applyPlaceholders(String templateBody, Map<String, String> placeholders) {
    String resolved = templateBody;
    if (placeholders == null || placeholders.isEmpty()) {
      return resolved;
    }
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      String value = entry.getValue() == null ? "" : entry.getValue();
      resolved = resolved.replace("{{" + entry.getKey() + "}}", value);
    }
    return resolved;
  }

  public enum MessageTemplate {
    JOBS_NOTIFY("jobsNotify.txt"),
    TOKEN("token.txt"),
    APPROVE_ACCOUNT("approveAccount.txt"),
    ACCOUNT_REJECTED("accountRejected.txt"),
    ACCOUNT_APPROVED("accountApproved.txt");

    private final String fileName;

    MessageTemplate(String fileName) {
      this.fileName = fileName;
    }
  }
}
