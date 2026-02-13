package com.jobshunter.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties
public class ApplicationProperties {

  private JobsHunter jobsHunter = new JobsHunter();
  private Twilio twilio = new Twilio();
  private Gpt gpt = new Gpt();
  private Grok grok = new Grok();
  private Gemini gemini = new Gemini();
  private Serp serp = new Serp();
  private Spring spring = new Spring();
  private IpInfo ipInfo = new IpInfo();

  /**
   * Convenience method for backward compatibility.
   * Returns spring.security properties.
   */
  public Security getSecurity() {
    return spring.getSecurity();
  }

  @Data
  @ConfigurationProperties(prefix = "jobshunter")
  public static class JobsHunter {

    private String name;
    private Scheduler scheduler = new Scheduler();
    private RateLimitPolicy rateLimit = new RateLimitPolicy();
    private String blacklist;
    private String blacklistJobsCompanySearch;
    private String expiredExpressions;
    private String localJobExpressions;
    private String freelancerExpressions;
    private String remoteExpressions;
    private AppThreads threads = new AppThreads();
    private AdditionalEffort additionalEffort = new AdditionalEffort();
  }

  @Data
  public static class AdditionalEffort {

    private int maxRetries = 3;
  }

  @Data
  public static class AppThreads {

    private int urlFetchRestClient;
    private int urlFetchPlaywright;
    private int jobProcessing;
    private int orders;
    private int notifications;
    private int maintenance;
    private int async;

  }

  @Data
  @ConfigurationProperties(prefix = "spring")
  public static class Spring {

    private Security security = new Security();

  }

  @Data
  public static class Security {

    @JsonProperty("jwt")
    private JwtProperties jwt;

    @JsonProperty("refresh-token")
    private RefreshTokenProperties refreshToken;

    @JsonProperty("cookie")
    private CookieProperties cookie;

    @JsonProperty("cors")
    private CorsProperties cors = new CorsProperties();

    @JsonProperty("httpsOnly")
    private HttpsOnlyProperties httpsOnly = new HttpsOnlyProperties();

  }

  @Data
  public static class HttpsOnlyProperties {

    private boolean enabled = true;

  }

  @Data
  public static class JwtProperties {

    private String secret;

    private long expirationSec;

  }

  @Data
  public static class RefreshTokenProperties {

    private int expirationSec;

    private String pepper;

  }

  @Data
  public static class CookieProperties {

    private DeviceIdProperties deviceId;

  }

  @Data
  public static class DeviceIdProperties {

    private int expirationSec;

  }

  @Data
  public static class CorsProperties {

    private List<String> allowedOrigins = List.of();

    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private List<String> allowedHeaders = List.of("*");

    private boolean allowCredentials = true;

    private long maxAgeSec = 3600;

  }

  @Data
  public static class Scheduler {

    private String processOrderFrequency;

    private String notifyUsersFrequency;

    private String cleanupFiles;

    private boolean enabled;

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
    private boolean enabled;
    private int threads;
  }

  @Data
  @ConfigurationProperties(prefix = "ipinfo")
  public static class IpInfo {

    private String apiKey;
    private boolean enabled;
    private int threads;

  }

  @Data
  @ConfigurationProperties(prefix = "grok")
  public static class Grok {

    private String apiKey;
    private boolean enabled;
    private int threads;
  }

  @Data
  @ConfigurationProperties(prefix = "gemini")
  public static class Gemini {

    private String apiKey;
    private boolean enabled;
    private int threads;
  }

  @Data
  @ConfigurationProperties(prefix = "serp")
  public static class Serp {

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
