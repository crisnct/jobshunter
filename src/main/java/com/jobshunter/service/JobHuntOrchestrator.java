package com.jobshunter.service;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.model.CvProfile;
import com.jobshunter.model.JobHuntSummary;
import com.jobshunter.model.JobOpportunity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class JobHuntOrchestrator {

    private final CvParserService cvParserService;
    private final RemoteJobApiClient remoteJobApiClient;
    private final JobMatcherService jobMatcherService;
    private final WhatsAppNotifier whatsAppNotifier;

    private final AtomicReference<String> promptRef;
    private final AtomicReference<Path> cvPathRef;
    private final AtomicReference<JobHuntSummary> lastRun = new AtomicReference<>();

    public JobHuntOrchestrator(ApplicationProperties properties,
                               CvParserService cvParserService,
                               RemoteJobApiClient remoteJobApiClient,
                               JobMatcherService jobMatcherService,
                               WhatsAppNotifier whatsAppNotifier) {
        this.cvParserService = cvParserService;
        this.remoteJobApiClient = remoteJobApiClient;
        this.jobMatcherService = jobMatcherService;
        this.whatsAppNotifier = whatsAppNotifier;
        this.promptRef = new AtomicReference<>(properties.getPrompt());
        this.cvPathRef = new AtomicReference<>(Path.of(properties.getCvPath()));
    }

    @Scheduled(cron = "${jobshunter.scheduler.cron:0 0 9 * * *}")
    public void scheduledRun() {
        log.info("Running scheduled job hunt...");
        runInternal(promptRef.get(), cvPathRef.get());
    }

    public JobHuntSummary runOnce(String prompt, String cvPath) {
        promptRef.set(prompt);
        Path path = Path.of(cvPath);
        cvPathRef.set(path);
        return runInternal(prompt, path);
    }

    public JobHuntSummary lastRun() {
        return lastRun.get();
    }

    private JobHuntSummary runInternal(String prompt, Path cvPath) {
        CvProfile profile = cvParserService.parse(cvPath);
        List<JobOpportunity> jobs = remoteJobApiClient.search(prompt);
        List<JobOpportunity> ranked = jobMatcherService.rank(profile, jobs);
        whatsAppNotifier.send(ranked);
        JobHuntSummary summary = new JobHuntSummary(prompt, cvPath.toString(), ranked);
        lastRun.set(summary);
        return summary;
    }
}
