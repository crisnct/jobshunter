package com.jobshunter.service.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobshunter.model.EngineType;
import com.jobshunter.service.application.hunting.hunters.PerplexityJobHunting;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.perplexity.PerplexityV1JobSearchImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the application with {@code perplexity.enabled=true}: the real client replaces the fake one and the hunter finds its models in
 * {@code ai_models} (it fails the startup otherwise). The {@code enabled=false} wiring is exercised by every other context test of the profile
 * {@code test}.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:perplexitywiring;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.defer-datasource-initialization=true",
        "spring.liquibase.enabled=false",
        "spring.sql.init.mode=always",
        "spring.sql.init.data-locations=classpath:data.sql",
        "perplexity.enabled=true",
        "perplexity.apiKey=not-a-real-key"
    }
)
@Import(SqlTestDataInitializer.class)
@ActiveProfiles("test")
class PerplexityWiringTest {

  @Autowired
  @Qualifier("JobsClientPERPLEXITY")
  private AiJobsClient<?> client;

  @Autowired
  private PerplexityJobHunting hunting;

  @Test
  void enabledPropertyActivatesTheRealClient() {
    assertThat(client).isInstanceOf(PerplexityV1JobSearchImpl.class);
  }

  @Test
  void hunterIsRegisteredForThePerplexityEngine() {
    assertThat(hunting.getEngineType()).isEqualTo(EngineType.PERPLEXITY);
  }
}
