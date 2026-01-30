package com.jobshunter.controller;

import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.database.service.UserCvDBService;
import com.jobshunter.service.application.UserCvService;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CvController {

  private final UserCvService userCvService;
  private final UserCvDBService userCvDBService;

  @PostMapping(value = "/upload", consumes = "multipart/form-data")
  public ResponseEntity<?> uploadCvToChatGpt(
      @RequestParam("file")
      @NotNull MultipartFile file,
      Authentication authentication
  ) throws IOException {
    return ResponseEntity.ok(userCvService.uploadUserCv(authentication.getName(), file));
  }

  @DeleteMapping
  public ResponseEntity<?> deleteCv(Authentication authentication) {
    userCvService.deleteUserCv(authentication.getName());
    return ResponseEntity.ok(Map.of("message", "CV deleted successfully"));
  }

  @GetMapping("/download")
  public ResponseEntity<?> downloadCv(Authentication authentication) {
    Optional<UserCvEntity> cvOptional = userCvDBService.getUserCv(authentication.getName());
    if (cvOptional.isEmpty() || cvOptional.get().getByteArray() == null) {
      return ResponseEntity.notFound().build();
    }
    UserCvEntity cv = cvOptional.get();
    ByteArrayResource resource = new ByteArrayResource(cv.getByteArray());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + cv.getFilename() + "\"")
        .contentType(MediaType.APPLICATION_PDF)
        .contentLength(resource.contentLength())
        .body(resource);
  }

}
