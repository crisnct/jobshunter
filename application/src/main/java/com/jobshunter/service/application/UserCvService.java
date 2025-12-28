package com.jobshunter.service.application;

import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.exceptions.ValidationException;
import com.jobshunter.service.clients.FileClient;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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

  private final AtomicBoolean FIRST_TIME = new AtomicBoolean(false);

  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
      MediaType.APPLICATION_PDF_VALUE,
      "application/msword",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      MediaType.TEXT_PLAIN_VALUE
  );

  private final UserDataService userDataService;

  private final FileClient gptFileClient;

  private final FileClient geminiFileClient;

  public UserCvService(
      UserDataService userDataService,
      @Qualifier("Gpt") FileClient gptFileClient,
      @Qualifier("Gemini") FileClient geminiFileClient
  ) {
    this.userDataService = userDataService;
    this.gptFileClient = gptFileClient;
    this.geminiFileClient = geminiFileClient;
  }

  @Transactional
  public String uploadUserCv(String username, MultipartFile file) throws IOException {
    if (FIRST_TIME.compareAndExchange(false, true)) {
      cleanupOldCVs();
    }
    if (!StringUtils.hasText(username)) {
      throw new ValidationException("User not authenticated");
    }
    if (file == null || file.isEmpty()) {
      throw new ValidationException("CV file in pdf format is required");
    }
    validateFile(file);

    UserEntity user = userDataService.getUser(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    Path tempFile = Files.createTempFile("cv-" + username + "-", resolveSafeSuffix(file.getOriginalFilename()));
    try {
      copyWithLimit(file.getInputStream(), tempFile, MAX_CV_BYTES);

      if (user.getCv() != null) {
        deleteRemoteFiles(user.getCv());
      }

      byte[] cvContent = Files.readAllBytes(tempFile);

      String gptFileId = gptFileClient.uploadFile(tempFile);
      if (!StringUtils.hasText(gptFileId)) {
        throw new RuntimeException("Failed to upload file to GPT "+tempFile.getFileName());
      }
      userDataService.replaceUserCv(user, cvContent, gptFileId);
      return gptFileId;
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
      throw new ValidationException("User not authenticated");
    }
    UserEntity user = userDataService.getUser(username)
        .orElseThrow(() -> new ValidationException("User not found"));
    if (user.getCv() != null) {
      deleteRemoteFiles(user.getCv());
      userDataService.deleteUserCv(user);
    }
  }

  private void validateFile(MultipartFile file) {
    String contentType = file.getContentType();
    if (StringUtils.hasText(contentType) && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
      throw new ValidationException("Unsupported CV content type");
    }
    long size = file.getSize();
    if (size > MAX_CV_BYTES) {
      throw new ValidationException("CV file exceeds 10MB limit");
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
          throw new ValidationException("CV file exceeds 10MB limit");
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

  private void deleteRemoteFiles(@NotNull UserCvEntity cv) {
    if (StringUtils.hasText(cv.getGptFileId())) {
      try {
        gptFileClient.deleteFile(cv.getGptFileId());
      } catch (Exception e) {
        log.error("Can not delete file from GPT: " + cv.getGptFileId(), e);
      }
    }
  }

  private void cleanupOldCVs() {
    log.info("Cleanup old cv's from gpt which are not used...");
    List<String> gptFileIdUsed = userDataService.getAllUsers().stream()
        .filter(user -> user.getCv() != null)
        .map(user -> user.getCv().getGptFileId())
        .toList();
    if (!gptFileIdUsed.isEmpty()) {
      gptFileClient.deleteAllFilesExcept(gptFileIdUsed);

    }
  }
}
