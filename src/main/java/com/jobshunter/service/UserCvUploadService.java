package com.jobshunter.service;

import com.jobshunter.database.entities.User;
import com.jobshunter.database.repository.UserRepository;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCvUploadService {

  private final UserRepository userRepository;
  private final ChatGptJobApiClient chatGptJobApiClient;

  @Transactional
  public String uploadUserCv(String username, MultipartFile file) throws IOException {
    if (!StringUtils.hasText(username)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
    }
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CV file is required");
    }

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    Path tempFile = Path.of(file.getOriginalFilename());
    try {
      Files.write(tempFile, file.getBytes());

      if (StringUtils.hasText(user.getCvFileId())) {
        chatGptJobApiClient.deleteFile(user.getCvFileId());
      }

      String uploadedFileId = chatGptJobApiClient.uploadFile(tempFile);
      if (!StringUtils.hasText(uploadedFileId)) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to upload CV to ChatGPT");
      }

      user.setCvFileId(uploadedFileId);
      userRepository.save(user);
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
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    if (StringUtils.hasText(user.getCvFileId())) {
      chatGptJobApiClient.deleteFile(user.getCvFileId());
      user.setCvFileId(null);
      userRepository.save(user);
    }
  }
}
