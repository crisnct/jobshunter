package com.jobshunter.service.clients;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.gemini.GeminiFileClientImpl;
import com.jobshunter.service.clients.gpt.GptFileClientImpl;
import com.jobshunter.testdata.DummyFileApiClient;
import java.io.IOException;
import java.nio.file.Path;

@PackageExpected("com.jobshunter.service.application")
public sealed interface FileClient permits GeminiFileClientImpl, GptFileClientImpl, DummyFileApiClient {

  String uploadFile(Path cvPath) throws IOException;

  void deleteFile(String fileId);
}
