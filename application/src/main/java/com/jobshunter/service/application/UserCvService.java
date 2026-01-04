package com.jobshunter.service.application;

import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.exceptions.ValidationException;
import com.jobshunter.model.EngineType;
import com.jobshunter.service.clients.FileClient;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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

  private final UserDataService userDataService;

  private final FileClient gptFileClient;

  private final FileClient geminiFileClient;

  private final FileClient grokFileClient;

  public UserCvService(
      UserDataService userDataService,
      @Qualifier("Gpt") FileClient gptFileClient,
      @Qualifier("Gemini") FileClient geminiFileClient,
      @Qualifier("Grok") FileClient grokFileClient
  ) {
    this.userDataService = userDataService;
    this.gptFileClient = gptFileClient;
    this.geminiFileClient = geminiFileClient;
    this.grokFileClient = grokFileClient;
  }

  @Transactional
  public Map<EngineType, String> uploadUserCv(String username, MultipartFile file) throws IOException {
    log.info("Uploading CV for user {}...", username);
    if (!StringUtils.hasText(username)) {
      throw new ValidationException("User not authenticated");
    }
    if (file == null || file.isEmpty()) {
      throw new ValidationException("CV file in pdf format is required");
    }
    validateFile(file);

    //noinspection OptionalGetWithoutIsPresent
    UserEntity user = userDataService.getUser(username).get();

    Path tempFile = Files.createTempFile("cv-" + username + "-", resolveSafeSuffix(file.getOriginalFilename()));
    try {
      copyWithLimit(file.getInputStream(), tempFile, MAX_CV_BYTES);

      if (user.getCv() != null) {
        deleteRemoteFiles(user.getCv());
      }

      byte[] cvContent = Files.readAllBytes(tempFile);

      Map<EngineType, String> result = new LinkedHashMap<>();
      String gptFileId = gptFileClient.uploadFile(tempFile);
      if (!StringUtils.hasText(gptFileId)) {
        throw new RuntimeException("Failed to upload file to GPT " + tempFile.getFileName());
      }
      String grokFileId = grokFileClient.uploadFile(tempFile);
      if (!StringUtils.hasText(grokFileId)) {
        throw new RuntimeException("Failed to upload file to GROK " + tempFile.getFileName());
      }
      result.put(EngineType.GPT, gptFileId);
      result.put(EngineType.GROK, grokFileId);

      userDataService.replaceUserCv(user, cvContent, gptFileId, grokFileId);
      log.info("CV uploaded for user {}", username);
      return result;
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
    UserEntity user = userDataService.getUser(username).orElseThrow(() -> new ValidationException("User not found"));
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

  public void cleanupOldCVs() {
    log.info("Cleanup old cv's from gpt/gemini/grok which are not used...");

    ProviderFiles gpt = new ProviderFiles(gptFileClient);
    ProviderFiles gemini = new ProviderFiles(geminiFileClient);
    ProviderFiles grok = new ProviderFiles(grokFileClient);

    for (UserEntity user : userDataService.getAllUsers()) {
      UserCvEntity cv = user.getCv();
      if (cv == null) {
        continue;
      }

      gpt.addIfPresent(cv.getGptFileId());
      gemini.addIfPresent(cv.getGeminiFileId());
      grok.addIfPresent(cv.getGrokFileId());
    }

    gpt.cleanup();
    gemini.cleanup();
    grok.cleanup();
  }

  private static final class ProviderFiles {

    private final FileClient client;

    private final List<String> fileIds = new ArrayList<>();

    private ProviderFiles(FileClient client) {
      this.client = client;
    }

    void addIfPresent(String fileId) {
      if (Strings.isNotBlank(fileId)) {
        fileIds.add(fileId);
      }
    }

    void cleanup() {
      client.deleteAllFilesExcept(fileIds);
    }
  }


}
