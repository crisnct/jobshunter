package com.jobshunter.service.clients.fileUpload;

import com.jobshunter.config.PackageExpected;
import java.io.IOException;
import java.nio.file.Path;

@PackageExpected("com.jobshunter.service.application")
public sealed interface GPTFilesApiClient permits ChatGptFileClient {

  String uploadFile(Path cvPath) throws IOException;

  boolean deleteFile(String fileId);
}
