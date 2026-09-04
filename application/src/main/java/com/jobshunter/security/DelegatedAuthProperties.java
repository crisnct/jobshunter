package com.jobshunter.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jobshunter.security.delegated-auth")
public record DelegatedAuthProperties(
    String issuerUri,
    String audience,
    String jwksUri,
    Duration jwksConnectTimeout,
    Duration jwksReadTimeout,
    String requiredTokenUse
) {
}
