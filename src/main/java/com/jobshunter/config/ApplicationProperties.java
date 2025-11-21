package com.jobshunter.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jobshunter")
public class ApplicationProperties {

  @Value("${jobshunter.expiredKeywords}")
  private String expiredKeywords;

  @Value("${jobshunter.iterationPerUser}")
  private int iterationPerUser;

  @Value("${jobshunter.iterationDelay}")
  private long iterationDelay;

  private Scheduler scheduler = new Scheduler();

  private WhatsApp whatsapp = new WhatsApp();

  private ChatGpt chatgpt = new ChatGpt();

  @Data
  public static class Scheduler {

    private String frequency;
  }

  @Data
  public static class WhatsApp {

    private String accountSid;
    private String authToken;
    private String fromNumber;
    private String toNumber;
  }

  @Data
  public static class ChatGpt {

    private String apiKey;
    private String model;
    private double temperature;
    private int maxTokens;
    private String toolsType;
  }
}
