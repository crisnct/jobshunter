package com.jobshunter.controller;

import com.jobshunter.dto.JobHuntSummary;
import com.jobshunter.dto.JobSearchRequest;
import com.jobshunter.service.JobHuntOrchestrator;
import com.jobshunter.service.UserJobService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class JobSearchController {

    private final JobHuntOrchestrator orchestrator;
    private final UserJobService userJobService;

    @PostMapping("/search")
    public JobHuntSummary search(@Valid @RequestBody JobSearchRequest request, Authentication authentication) throws IOException, InterruptedException {
        String username = authentication != null ? authentication.getName() : null;
        List<String> existingUrls = userJobService.getExistingJobUrlsForUser(username);
        String promptForChatGpt = request.prompt();
        if (!existingUrls.isEmpty()) {
            promptForChatGpt = promptForChatGpt + ".  Exclude those url's: " + String.join(", ", existingUrls) + ".";
        }

        JobHuntSummary summary = orchestrator.runOnce(promptForChatGpt, request.cvPath(), username);
        userJobService.saveJobsForUser(username, summary.jobsFound());
        return summary;
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return Optional.ofNullable(orchestrator.lastRun())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(Map.of("message", "No search executed yet")));
    }
}
