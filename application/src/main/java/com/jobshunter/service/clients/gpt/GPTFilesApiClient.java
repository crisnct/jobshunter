package com.jobshunter.service.clients.gpt;

import com.jobshunter.processor.PackageExpected;
import java.io.IOException;
import java.nio.file.Path;

@PackageExpected("com.jobshunter.service.application")
public sealed interface GPTFilesApiClient permits GptFileClient {

  String uploadFile(Path cvPath) throws IOException;

  boolean deleteFile(String fileId);
}
