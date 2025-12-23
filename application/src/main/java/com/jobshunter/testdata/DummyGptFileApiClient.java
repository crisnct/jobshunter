package com.jobshunter.testdata;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.FileClient;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("Gpt")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "jobshunter.useDummyData", havingValue = "true")
public final class DummyGptFileApiClient implements FileClient {

  @Override
  public String uploadFile(Path cvPath) {
    log.info("File {} uploaded properly", cvPath);
    return "uploaded";
  }

  @Override
  public void deleteFile(String fileId) {
    log.info("File {} deleted", fileId);
  }
}
