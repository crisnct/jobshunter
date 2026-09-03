package com.jobshunter.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jobshunter.security.delegated-auth")
public record DelegatedAuthProperties(
    String issuerUri,
    String audience,
    String jwksUri,
    String requiredScope,
    String requiredTokenUse
) {
}
