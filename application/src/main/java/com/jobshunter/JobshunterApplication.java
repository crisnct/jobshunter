package com.jobshunter;

import com.jobshunter.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class})
public class JobshunterApplication {

  static void main(String[] args) {
    SpringApplication.run(JobshunterApplication.class, args);
  }
}
