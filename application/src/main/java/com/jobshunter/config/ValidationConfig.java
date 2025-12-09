package com.jobshunter.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

@Configuration
public class ValidationConfig {

  @Bean
  public Validator jsrValidator() {
    return Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Bean
  @Primary
  public org.springframework.validation.Validator validator(Validator jsrValidator) {
    return new SpringValidatorAdapter(jsrValidator);
  }

  @Bean(name = "configurationPropertiesValidator")
  public org.springframework.validation.Validator configurationPropertiesValidator(
      @Qualifier("validator") org.springframework.validation.Validator springValidator) {
    return springValidator;
  }
}
