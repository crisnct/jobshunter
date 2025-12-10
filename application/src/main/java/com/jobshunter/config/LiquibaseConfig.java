package com.jobshunter.config;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class LiquibaseConfig {

  @Bean
  public SpringLiquibase liquibase(DataSource dataSource, Environment env) {
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setChangeLog(
        env.getProperty("spring.liquibase.change-log", "classpath:db/changelog/db.changelog-master.xml"));
    liquibase.setShouldRun(env.getProperty("spring.liquibase.enabled", Boolean.class, true));
    liquibase.setDefaultSchema(env.getProperty("spring.liquibase.default-schema"));
    liquibase.setContexts(env.getProperty("spring.liquibase.contexts"));
    liquibase.setLabels(env.getProperty("spring.liquibase.labels"));
    return liquibase;
  }
}
