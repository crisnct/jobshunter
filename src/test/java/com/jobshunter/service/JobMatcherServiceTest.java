package com.jobshunter.service;

import com.jobshunter.model.CvProfile;
import com.jobshunter.model.JobOpportunity;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JobMatcherServiceTest {

    private final JobMatcherService service = new JobMatcherService();

    @Test
    void ranksJobsByKeywordMatches() {
        CvProfile profile = new CvProfile(null, "", Set.of("java", "cloud", "spring"));
        JobOpportunity a = new JobOpportunity("Java Developer", "ACME", "Remote",
                URI.create("https://example.com/a"), OffsetDateTime.now(), List.of(), "Java spring microservices");
        JobOpportunity b = new JobOpportunity("Python Developer", "ACME", "Remote",
                URI.create("https://example.com/b"), OffsetDateTime.now(), List.of(), "python data science");

        List<JobOpportunity> ranked = service.rank(profile, List.of(b, a));
        assertThat(ranked.get(0)).isEqualTo(a);
    }
}
