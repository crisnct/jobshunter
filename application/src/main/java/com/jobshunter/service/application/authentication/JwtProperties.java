package com.jobshunter.service.application.authentication;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String secret;

    private long expirationMs;

}
