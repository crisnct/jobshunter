package com.jobshunter.service.application;

import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.exceptions.ValidationException;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.ResumeFileInfo;
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

  private final Map<EngineType, FileClient> clients;

  public UserCvService(
      UserDataService userDataService,
      @Qualifier("Gpt") FileClient gptFileClient,
      @Qualifier("Gemini") FileClient geminiFileClient,
      @Qualifier("Grok") FileClient grokFileClient
  ) {
    this.userDataService = userDataService;
    this.clients = Map.of(
        EngineType.GPT, gptFileClient,
        EngineType.GEMINI, geminiFileClient,
        EngineType.GROK, grokFileClient
    );
  }

  // ---------------------------------------------------------------------------
  // Upload
  // ---------------------------------------------------------------------------

  @Transactional
  public Map<EngineType, ResumeFileInfo> uploadUserCv(String username, MultipartFile file) throws IOException {
    log.info("Uploading CV for user {}...", username);
    if (file == null || file.isEmpty()) {
      throw new ValidationException("CV file is required");
    }
    this.validateFile(file);

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    UserEntity user = userDataService.getUser(username).get();

    Path tempFile = Files.createTempFile("cv-" + username + "-", resolveSafeSuffix(file.getOriginalFilename()));
    try {
      copyWithLimit(file.getInputStream(), tempFile, MAX_CV_BYTES);
      if (user.getCv() != null) {
        deleteRemoteFiles(user.getCv());
      }
      byte[] cvContent = Files.readAllBytes(tempFile);

      Map<EngineType, ResumeFileInfo> uploadedResults = new LinkedHashMap<>();
      for (Map.Entry<EngineType, FileClient> entry : clients.entrySet()) {
        EngineType engine = entry.getKey();
        FileClient client = entry.getValue();
        ResumeFileInfo fileInfo = client.uploadFile(tempFile);
        if (fileInfo == null || !StringUtils.hasText(fileInfo.fileId())) {
          throw new IllegalStateException("Failed to upload CV to " + engine + ": " + tempFile.getFileName());
        }
        uploadedResults.put(engine, fileInfo);
      }

      userDataService.replaceUserCv(user, cvContent, uploadedResults);
      log.info("CV uploaded successfully for user {}", username);
      return uploadedResults;
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException ex) {
        log.warn("Failed to delete temp CV file {}: {}", tempFile, ex.getMessage());
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Delete single user CV
  // ---------------------------------------------------------------------------

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

  // ---------------------------------------------------------------------------
  // Cleanup unused CVs (global)
  // ---------------------------------------------------------------------------

  public void cleanupOldCVs() {
    log.info("Cleanup old CVs from all engines...");

    Map<EngineType, ProviderFiles> providers = new LinkedHashMap<>();
    clients.forEach((engine, client) ->
        providers.put(engine, new ProviderFiles(client))
    );

    for (UserEntity user : userDataService.getAllUsers()) {
      UserCvEntity cv = user.getCv();
      if (cv == null) {
        continue;
      }

      for (EngineType engine : providers.keySet()) {
        userDataService.getRemoteCvFileId(cv, engine)
            .ifPresent(providers.get(engine)::addIfPresent);
      }
    }

    providers.values().forEach(ProviderFiles::cleanup);
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private void deleteRemoteFiles(@NotNull UserCvEntity cv) {
    for (Map.Entry<EngineType, FileClient> entry : clients.entrySet()) {
      EngineType engine = entry.getKey();
      FileClient client = entry.getValue();

      userDataService.getRemoteCvFileId(cv, engine)
          .ifPresent(fileId -> deleteIfPresent(fileId, client, engine));
    }
  }

  private void deleteIfPresent(String fileId, FileClient client, EngineType engine) {
    if (!StringUtils.hasText(fileId)) {
      return;
    }
    try {
      client.deleteFile(fileId);
    } catch (Exception e) {
      log.error("Cannot delete file from {}: {}", engine, fileId, e);
    }
  }

  private void validateFile(MultipartFile file) {
    String contentType = file.getContentType();
    if (StringUtils.hasText(contentType) && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
      throw new ValidationException("Unsupported CV content type");
    }
    if (file.getSize() > MAX_CV_BYTES) {
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

  // ---------------------------------------------------------------------------
  // Internal helper class
  // ---------------------------------------------------------------------------

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
