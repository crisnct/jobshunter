package com.jobshunter.controller;

import com.jobshunter.service.application.UserCvService;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
public class CvController {

  private final UserCvService userCvService;

  @PostMapping(value = "/upload", consumes = "multipart/form-data")
  public ResponseEntity<?> uploadCvToChatGpt(
      @RequestParam("file")
      @NotNull MultipartFile file,
      Authentication authentication
  ) throws IOException {
    String username = authentication != null ? authentication.getName() : null;
    String fileId = userCvService.uploadUserCv(username, file);
    return ResponseEntity.ok(Map.of("message", "CV uploaded to ChatGPT successfully", "fileId", fileId));
  }

  @DeleteMapping
  public ResponseEntity<?> deleteCv(Authentication authentication) {
    String username = authentication != null ? authentication.getName() : null;
    userCvService.deleteUserCv(username);
    return ResponseEntity.ok(Map.of("message", "CV deleted successfully"));
  }
}
