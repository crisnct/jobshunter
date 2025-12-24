package com.jobshunter.service.clients;

import com.jobshunter.dto.Job;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UrlExtractor {

  private static final Pattern URL_PATTERN = Pattern.compile(
      "\\b((?:https?://|www\\.)" +
          "[\\w\\-]+(\\.[\\w\\-]+)+" +
          "(?:[/?#][^\\s\"'<>]*)?)",
      Pattern.CASE_INSENSITIVE
  );

  public List<Job> parseJobs(String text) {
    if (Strings.isBlank(text)) {
      return List.of();
    }
    List<Job> results = new ArrayList<>();
    Matcher matcher = URL_PATTERN.matcher(text);
    while (matcher.find()) {
      String url = normalize(matcher.group(1));
      results.add(new Job(-1, url, ""));
    }
    return results;
  }

  private String normalize(String url) {
    // normalize www.* → https://www.*
    if (url.startsWith("www.")) {
      return "https://" + url;
    }
    return url;
  }

}
