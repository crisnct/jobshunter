package com.jobshunter.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jobshunter.security.delegated-auth")
public record DelegatedAuthProperties(
    boolean enabled,
    String issuerUri,
    String audience,
    String requiredScope
) {
}
