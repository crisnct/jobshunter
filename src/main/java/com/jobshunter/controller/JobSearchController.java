package com.jobshunter.controller;

import com.jobshunter.dto.JobHuntSummary;
import com.jobshunter.dto.JobSearchRequest;
import com.jobshunter.service.JobHuntOrchestrator;
import com.jobshunter.service.UserCvService;
import com.jobshunter.service.UserJobService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class JobSearchController {

    private final JobHuntOrchestrator orchestrator;
    private final UserJobService userJobService;
    private final UserCvService userCvService;

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

    @PostMapping(value = "/cv", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadCv(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "CV file is required"));
        }
        String username = authentication != null ? authentication.getName() : null;
        userCvService.saveCv(username, file.getBytes(), file.getOriginalFilename());
        return ResponseEntity.ok(Map.of("message", "CV uploaded successfully"));
    }

    @GetMapping(value = "/cv", produces = "application/pdf")
    public ResponseEntity<byte[]> getCv(@RequestParam("filename") String filename, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        UserCvService.CvFile cv = userCvService.getCv(username, filename);
        return ResponseEntity.ok()
            .header("Content-Disposition", "inline; filename=\"" + (cv.filename() != null ? cv.filename() : "cv.pdf") + "\"")
            .body(cv.data());
    }

    @DeleteMapping("/cv")
    public ResponseEntity<?> deleteCv(@RequestParam("filename") String filename, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        userCvService.deleteCvByFilename(username, filename);
        return ResponseEntity.ok(Map.of("message", "CV deleted successfully"));
    }

    @GetMapping("/cv/list")
    public ResponseEntity<List<String>> listCv(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(userCvService.listFilenames(username));
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return Optional.ofNullable(orchestrator.lastRun())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(Map.of("message", "No search executed yet")));
    }
}
