package com.jobshunter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties
public class ApplicationProperties {

  private JobsHunter jobsHunter = new JobsHunter();
  private Twilio twilio = new Twilio();
  private Gpt gpt = new Gpt();
  private SerpApi serpApi = new SerpApi();

  @Data
  @ConfigurationProperties(prefix = "jobshunter")
  public static class JobsHunter {

    private String name;
    private String expiredExpressions;
    private Boolean useDummyData;
    private int iterationPerUser;
    private long iterationDelay;
    private Scheduler scheduler = new Scheduler();
  }

  @Data
  public static class Scheduler {

    private String frequency;
  }

  @Data
  @ConfigurationProperties(prefix = "twilio")
  public static class Twilio {

    private String accountSid;
    private String authToken;
    private String fromNumber;
  }

  @Data
  @ConfigurationProperties(prefix = "gpt")
  public static class Gpt {
    private String apiKey;
    private int maxTokens;
    private ModelSpecific economy;
    private ModelSpecific premium;
  }

  @Data
  public static class ModelSpecific {

    private String model;
    private String systemPromptFile;
  }

  @Data
  @ConfigurationProperties(prefix = "serpApi")
  public static class SerpApi {

    private String apiKey;

    private int maxPageSearch;
  }

}
