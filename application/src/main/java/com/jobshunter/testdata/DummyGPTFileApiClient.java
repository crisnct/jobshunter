package com.jobshunter.testdata;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.GPTFileApiClient;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "jobshunter.useDummyData", havingValue = "true")
public final class DummyGPTFileApiClient implements GPTFileApiClient {

  @Override
  public String uploadFile(Path cvPath) {
    return "File uploaded properly";
  }

  @Override
  public boolean deleteFile(String fileId) {
    return true;
  }
}
