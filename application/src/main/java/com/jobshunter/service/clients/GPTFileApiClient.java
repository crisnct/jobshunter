package com.jobshunter.service.clients;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.gpt.GptFileApiClientImpl;
import com.jobshunter.testdata.DummyGPTFileApiClient;
import java.io.IOException;
import java.nio.file.Path;

@PackageExpected("com.jobshunter.service.application")
public sealed interface GPTFileApiClient permits GptFileApiClientImpl, DummyGPTFileApiClient {

  String uploadFile(Path cvPath) throws IOException;

  boolean deleteFile(String fileId);
}
