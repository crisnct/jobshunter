package com.jobshunter.service.clients.fileUpload;

import java.io.IOException;
import java.nio.file.Path;

public sealed interface GPTFilesApiClient permits ChatGptFileClient {

  String uploadFile(Path cvPath) throws IOException;

  boolean deleteFile(String fileId);
}
