package com.jobshunter.service.application;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.config.ApplicationProperties.ChatGpt5;
import com.jobshunter.config.ApplicationProperties.Twilio;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.service.clients.TwillioClient;
import java.lang.reflect.Method;
import java.util.Properties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

class JobHuntServiceTest {

  private static final WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());

  private JobHuntService jobHuntService;

  private Method isValidJobMethod;

  @BeforeEach
  void setUp() throws Exception {
    if (!wireMockServer.isRunning()) {
      wireMockServer.start();
    }
    wireMockServer.resetAll();
    jobHuntService = new JobHuntService();

    ApplicationProperties properties = loadApplicationProperties();
    ReflectionTestUtils.setField(jobHuntService, "properties", properties);

    // request factory cu timeout-uri
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000);
    factory.setReadTimeout(15000);

    var restTemplate = new RestTemplate(factory);
    // interceptor exact ca în Spring Boot 3
    restTemplate.getInterceptors().add((request, body, execution) -> {
      var headers = request.getHeaders();
      headers.set("User-Agent", "Mozilla/5.0");
      headers.set("Accept-Language", "en-US,en;q=0.9");
      headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
      headers.set("Referer", "https://www.jobs-hunter.com");

      return execution.execute(request, body);
    });

    ReflectionTestUtils.setField(jobHuntService, "restTemplate", restTemplate);
    ReflectionTestUtils.setField(jobHuntService, "mapper", new JsonMapper());
    ReflectionTestUtils.setField(jobHuntService, "whatsAppNotifier", new NoOpNotifier());
    ReflectionTestUtils.setField(jobHuntService, "userDataService", new NoOpUserDataService());

    jobHuntService.init();

    isValidJobMethod = JobHuntService.class.getDeclaredMethod("isValidJob", String.class);
    isValidJobMethod.setAccessible(true);
  }

  @AfterEach
  void resetWireMock() {
    wireMockServer.resetAll();
  }

  @AfterAll
  static void stopWireMock() {
    wireMockServer.stop();
  }

  @Test
  void isValidJobUsesWireMockResponses() throws Exception {
    boolean builtinResult = invokeIsValidJob("https://builtin.com/job/senior-java-developer/7297380");
    boolean jobgetherResult = invokeIsValidJob(
        "https://jobgether.com/offer/687836fc57fb149ae379f3ce-senior-java-developer-for-timisoara-sibiu-m-f-d");

    Assertions.assertFalse(builtinResult, "Expected BuiltIn job to be considered valid");
    Assertions.assertFalse(jobgetherResult, "Expected Jobgether job to be marked as expired");
  }

  @Test
  void isValidRemoteRocketShip() throws Exception {
    Assertions.assertTrue(invokeIsValidJob(
            "https://www.remoterocketship.com/company/infotreeglobal/jobs/senior-java-developer-endur-murex-romania-remote/"),
        "Expected URL to be considered valid"
    );
  }

  private boolean invokeIsValidJob(String url) throws Exception {
    return (boolean) isValidJobMethod.invoke(jobHuntService, url);
  }

  private ApplicationProperties loadApplicationProperties() {
    YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
    factory.setResources(new ClassPathResource("application-test.yml"));
    Properties props = factory.getObject();

    ApplicationProperties result = new ApplicationProperties();
    if (props == null) {
      return result;
    }

    ReflectionTestUtils.setField(result, "expiredExpressions", props.getProperty("jobshunter.expiredExpressions"));

    String iterationPerUser = props.getProperty("jobshunter.iterationPerUser");
    if (StringUtils.hasText(iterationPerUser)) {
      ReflectionTestUtils.setField(result, "iterationPerUser", Integer.parseInt(iterationPerUser));
    }

    String iterationDelay = props.getProperty("jobshunter.iterationDelay");
    if (StringUtils.hasText(iterationDelay)) {
      ReflectionTestUtils.setField(result, "iterationDelay", Long.parseLong(iterationDelay));
    }

    // nested scheduler
    ApplicationProperties.Scheduler scheduler = result.getScheduler();
    String frequency = props.getProperty("jobshunter.scheduler.frequency");
    if (scheduler != null && StringUtils.hasText(frequency)) {
      ReflectionTestUtils.setField(scheduler, "frequency", frequency);
    }

    // nested WhatsApp
    Twilio whatsapp = result.getTwilio();
    if (whatsapp != null) {
      setIfPresent(props, "jobshunter.whatsapp.account-sid", whatsapp, "accountSid");
      setIfPresent(props, "jobshunter.whatsapp.auth-token", whatsapp, "authToken");
      setIfPresent(props, "jobshunter.whatsapp.from-number", whatsapp, "fromNumber");
      setIfPresent(props, "jobshunter.whatsapp.to-number", whatsapp, "toNumber");
    }

    // nested ChatGPT
    ChatGpt5 chatGpt5 = result.getChatgpt5();
    if (chatGpt5 != null) {
      setIfPresent(props, "jobshunter.chatGpt.apiKey", chatGpt5, "apiKey");
      setIfPresent(props, "jobshunter.chatGpt.model", chatGpt5, "model");
      setIfPresent(props, "jobshunter.chatGpt.toolsType", chatGpt5, "toolsType");

      String temp = props.getProperty("jobshunter.chatGpt.temperature");
      if (StringUtils.hasText(temp)) {
        ReflectionTestUtils.setField(chatGpt5, "temperature", Double.parseDouble(temp));
      }
      String maxTokens = props.getProperty("jobshunter.chatGpt.maxTokens");
      if (StringUtils.hasText(maxTokens)) {
        ReflectionTestUtils.setField(chatGpt5, "maxTokens", Integer.parseInt(maxTokens));
      }
    }

    return result;
  }

  private static void setIfPresent(Properties props, String key, Object target, String field) {
    String value = props.getProperty(key);
    if (StringUtils.hasText(value)) {
      ReflectionTestUtils.setField(target, field, value);
    }
  }

  private static class NoOpNotifier extends TwillioClient {

  }

  private static class NoOpUserDataService extends UserDataService {

  }


}
