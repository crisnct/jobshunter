package com.jobshunter;

import com.jobshunter.service.application.authentication.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({ApplicationProperties.class, JwtProperties.class})
public class JobshunterApplication {

  static void main(String[] args) {
    SpringApplication.run(JobshunterApplication.class, args);
  }
}
