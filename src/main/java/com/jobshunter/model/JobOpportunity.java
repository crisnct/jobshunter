package com.jobshunter.model;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

public record JobOpportunity(
        String title,
        String company,
        String location,
        URI url,
        OffsetDateTime publishedAt,
        List<String> tags,
        String description) {
}
