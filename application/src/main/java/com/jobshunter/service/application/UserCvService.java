package com.jobshunter.service.application;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.service.clients.fileUpload.GPTFilesApiClient;
import com.jobshunter.service.clients.jobSearch.ChatGptApi5Client;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class UserCvService {

  private static final long MAX_CV_BYTES = 10 * 1024 * 1024;

  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
      MediaType.APPLICATION_PDF_VALUE,
      "application/msword",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      MediaType.TEXT_PLAIN_VALUE
  );

  @Autowired
  private UserDataService userDataService;

  @Autowired
  private ChatGptApi5Client chatGptApiClient;

  @Autowired
  private GPTFilesApiClient filesClient;

  @Transactional
  public String uploadUserCv(String username, MultipartFile file) throws IOException {
    if (!StringUtils.hasText(username)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
    }
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CV file is required");
    }
    validateFile(file);

    UserEntity user = userDataService.getUser(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    Path tempFile = Files.createTempFile("cv-" + username + "-", resolveSafeSuffix(file.getOriginalFilename()));
    try {
      copyWithLimit(file.getInputStream(), tempFile, MAX_CV_BYTES);

      if (StringUtils.hasText(user.getCvFileId())) {
        filesClient.deleteFile(user.getCvFileId());
      }

      String uploadedFileId = filesClient.uploadFile(tempFile);
      if (!StringUtils.hasText(uploadedFileId)) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to upload CV to ChatGPT");
      }

      user.setCvFileId(uploadedFileId);
      userDataService.save(user);
      return uploadedFileId;
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException ex) {
        log.warn("Failed to delete temp CV file {}: {}", tempFile, ex.getMessage());
      }
    }
  }

  @Transactional
  public void deleteUserCv(String username) {
    if (!StringUtils.hasText(username)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
    }
    UserEntity user = userDataService.getUser(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    if (StringUtils.hasText(user.getCvFileId())) {
      filesClient.deleteFile(user.getCvFileId());
      user.setCvFileId(null);
      userDataService.save(user);
    }
  }

  private void validateFile(MultipartFile file) {
    String contentType = file.getContentType();
    if (StringUtils.hasText(contentType) && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported CV content type");
    }
    long size = file.getSize();
    if (size > MAX_CV_BYTES) {
      throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "CV file exceeds 5MB limit");
    }
  }

  private void copyWithLimit(InputStream source, Path target, long maxBytes) throws IOException {
    long copied = 0;
    byte[] buffer = new byte[8192];
    try (InputStream in = source; OutputStream out = Files.newOutputStream(target)) {
      int read;
      while ((read = in.read(buffer)) != -1) {
        copied += read;
        if (copied > maxBytes) {
          throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "CV file exceeds 5MB limit");
        }
        out.write(buffer, 0, read);
      }
    }
  }

  private String resolveSafeSuffix(String originalFilename) {
    if (!StringUtils.hasText(originalFilename)) {
      return ".tmp";
    }
    String clean = originalFilename.replace("\\", "/");
    int lastSlash = clean.lastIndexOf('/');
    if (lastSlash != -1) {
      clean = clean.substring(lastSlash + 1);
    }
    if (!clean.contains(".")) {
      clean = clean + ".tmp";
    }
    return "-" + clean;
  }
}
