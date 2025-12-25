package com.jobshunter.service.clients;

import com.jobshunter.model.Job;
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
      "(?i)\\bhttps?://[^\\s\\]\\[\\(\\)<>'\"`]+"
  );

  public List<Job> parseJobs(String text) {
    if (Strings.isBlank(text)) {
      return List.of();
    }
    List<Job> results = new ArrayList<>();
    Matcher matcher = URL_PATTERN.matcher(text);
    while (matcher.find()) {
      String url = normalize(matcher.group());
      results.add(new Job(-1, url, null));
    }
    return results;
  }

  private String normalize(String url) {
    // normalize www.* → https://www.*
    if (url.startsWith("www.")) {
      url = "https://" + url;
    }
    if (url.endsWith(")")) {
      url = url.substring(0, url.length() - 1);
    }
    return url;
  }

}
