package com.jobshunter;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties
public class ApplicationProperties {

  private JobsHunter jobsHunter = new JobsHunter();
  private Twilio twilio = new Twilio();
  private Gpt gpt = new Gpt();
  private Gemini gemini = new Gemini();
  private SerpApi serpApi = new SerpApi();

  @Data
  @ConfigurationProperties(prefix = "jobshunter")
  public static class JobsHunter {

    private String name;
    private String expiredExpressions;
    private Scheduler scheduler = new Scheduler();
    private RateLimitPolicy rateLimit = new RateLimitPolicy();
    private Boolean allowRedirection;
    private String blacklist;

  }

  @Data
  public static class Scheduler {

    private String processOrderFrequency;

    private String notifyUsersFrequency;

  }

  @Data
  @ConfigurationProperties(prefix = "twilio")
  public static class Twilio {

    private String accountSid;
    private String authToken;
    private String fromNumber;
    private boolean enabled;
  }

  @Data
  @ConfigurationProperties(prefix = "gpt")
  public static class Gpt {

    private String apiKey;
    private int maxTokens;
    private boolean enabled;
  }

  @Data
  @ConfigurationProperties(prefix = "gemini")
  public static class Gemini {

    private String apiKey;
    private int maxTokens;
    private boolean enabled;
  }

  @Data
  @ConfigurationProperties(prefix = "serpApi")
  public static class SerpApi {

    private String apiKey;
    private int maxPageSearch;
    private boolean enabled;
  }

  @Data
  public static class RateLimitPolicy {
    private long capacity;
    private Duration window;
  }

}
