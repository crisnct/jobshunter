package com.jobshunter.service.testdata;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.FileClient;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("Gpt")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "false")
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

  @Override
  public void deleteAllFilesExcept(List<String> fileIds) {
    log.info("Files unused deleted");
  }
}
