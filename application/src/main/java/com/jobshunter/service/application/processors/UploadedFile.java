package com.jobshunter.service.application.processors;

import com.jobshunter.service.clients.FileClient;
import java.io.IOException;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Wrapper class for managing uploaded files as an AutoCloseable resource. Automatically deletes the uploaded file when closed via
 * try-with-resources.
 */
@Slf4j
@RequiredArgsConstructor
public class UploadedFile implements AutoCloseable {

  private final FileClient fileClient;
  private final Path filePath;
  private String fileId;

  /**
   * Uploads the file and stores the returned file ID for later deletion.
   *
   * @return the file ID returned by the upload operation
   * @throws IOException if the upload fails
   */
  public String uploadAndGetFileId() throws IOException {
    fileId = fileClient.uploadFile(filePath);
    return fileId;
  }

  /**
   * Gets the file ID of the uploaded file.
   *
   * @return the file ID
   * @throws IOException if the file hasn't been uploaded yet
   */
  public String getFileId() throws IOException {
    if (fileId == null) {
      fileId = uploadAndGetFileId();
    }
    return fileId;
  }

  /**
   * Closes the resource and deletes the uploaded file.
   */
  @Override
  public void close() {
    if (fileId != null) {
      try {
        fileClient.deleteFile(fileId);
      } catch (Exception e) {
        log.warn("Failed to delete uploaded file with ID: {}", fileId, e);
      }
    }
  }
}