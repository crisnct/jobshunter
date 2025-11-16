package com.jobshunter.service;

import com.jobshunter.model.CvProfile;
import com.jobshunter.model.JobOpportunity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JobMatcherService {

    public List<JobOpportunity> rank(CvProfile profile, List<JobOpportunity> jobs) {
        Set<String> keywords = profile.keywords();
        return jobs.stream()
                .sorted((a, b) -> Integer.compare(score(b, keywords), score(a, keywords)))
                .limit(10)
                .collect(Collectors.toList());
    }

    private int score(JobOpportunity opportunity, Set<String> keywords) {
        String haystack = (opportunity.description() + " " + opportunity.title()).toLowerCase(Locale.ROOT);
        int matches = 0;
        for (String keyword : keywords) {
            if (haystack.contains(keyword.toLowerCase(Locale.ROOT))) {
                matches++;
            }
        }
        return matches;
    }
}
