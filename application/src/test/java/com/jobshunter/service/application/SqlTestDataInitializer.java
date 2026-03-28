package com.jobshunter.service.application;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.repository.AiModelRepository;
import com.jobshunter.model.EngineType;
import javax.sql.DataSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@TestConfiguration
public class SqlTestDataInitializer {

  @Bean
  InitializingBean populateTestData(DataSource dataSource) {
    return () -> {
      ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("data.sql"));
      populator.execute(dataSource);
    };
  }

  @Bean
  ApplicationRunner ensureModelSeeds(AiModelRepository aiModelRepository) {
    return args -> {
      seedIfMissing(aiModelRepository, EngineType.GEMINI, "gemini-2.5-flash");
      seedIfMissing(aiModelRepository, EngineType.GPT, "gpt-4o-mini-2024-07-18");
      seedIfMissing(aiModelRepository, EngineType.GPT, "gpt-5.1-2025-11-13");
      seedIfMissing(aiModelRepository, EngineType.GROK, "grok-4-fast-reasoning");
    };
  }

  private void seedIfMissing(AiModelRepository repo, EngineType type, String model) {
    repo.findByProviderAndModel(type, model)
        .orElseGet(() -> repo.save(new AiModelEntity(type, model)));
  }
}
