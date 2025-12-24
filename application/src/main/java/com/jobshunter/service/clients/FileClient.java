package com.jobshunter.service.clients;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.gemini.GeminiFileClientImpl;
import com.jobshunter.service.clients.gpt.GptFileClientImpl;
import com.jobshunter.service.testdata.DummyGeminiFileApiClient;
import com.jobshunter.service.testdata.DummyGptFileApiClient;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@PackageExpected("com.jobshunter.service.application")
public sealed interface FileClient permits GeminiFileClientImpl, GptFileClientImpl, DummyGeminiFileApiClient, DummyGptFileApiClient {

  String uploadFile(Path cvPath) throws IOException;

  void deleteFile(String fileId);

  void deleteAllFilesExcept(@NotBlank List<String> fileIds);
}
