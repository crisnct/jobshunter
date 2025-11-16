package com.jobshunter.web;

import com.jobshunter.model.JobHuntSummary;
import com.jobshunter.model.JobSearchRequest;
import com.jobshunter.service.JobHuntOrchestrator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/job")
public class JobSearchController {

    private final JobHuntOrchestrator orchestrator;

    public JobSearchController(JobHuntOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/search")
    public JobHuntSummary search(@Valid @RequestBody JobSearchRequest request) {
        return orchestrator.runOnce(request.prompt(), request.cvPath());
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return Optional.ofNullable(orchestrator.lastRun())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(Map.of("message", "No search executed yet")));
    }
}
