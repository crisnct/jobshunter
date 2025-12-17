package com.jobshunter.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties
public class ApplicationProperties {

  private JobsHunter jobsHunter = new JobsHunter();
  private Twilio twilio = new Twilio();
  private Gpt5 gpt5 = new Gpt5();
  private Gpt4 gpt4 = new Gpt4();
  private SerpApi serpApi = new SerpApi();

  @Data
  @ConfigurationProperties(prefix = "jobshunter")
  public static class JobsHunter {

    private String expiredExpressions;
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
  public static class Gpt {

    private String apiKey;
    private String model;
    private int maxTokens;
    private String systemPromptFile;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @ConfigurationProperties(prefix = "chatGpt5")
  public static class Gpt5 extends Gpt {

    private double temperature;
    private String toolsType;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @ConfigurationProperties(prefix = "chatGpt4")
  public static class Gpt4 extends Gpt {

    private double temperature;
  }

  @Data
  @ConfigurationProperties(prefix = "serpApi")
  public static class SerpApi{

    private String apiKey;

    private int maxPageSearch;
  }

}
