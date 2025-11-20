package com.jobshunter.controller;

import com.jobshunter.service.UserCvUploadService;
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

  private final UserCvUploadService userCvUploadService;

  @PostMapping(value = "/upload", consumes = "multipart/form-data")
  public ResponseEntity<?> uploadCvToChatGpt(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
    String username = authentication != null ? authentication.getName() : null;
    String fileId = userCvUploadService.uploadUserCv(username, file);
    return ResponseEntity.ok(Map.of("message", "CV uploaded to ChatGPT successfully", "fileId", fileId));
  }

  @DeleteMapping
  public ResponseEntity<?> deleteCv(Authentication authentication) {
    String username = authentication != null ? authentication.getName() : null;
    userCvUploadService.deleteUserCv(username);
    return ResponseEntity.ok(Map.of("message", "CV deleted successfully"));
  }
}
