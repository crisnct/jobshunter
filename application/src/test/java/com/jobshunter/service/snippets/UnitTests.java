package com.jobshunter.service.snippets;

import com.jobshunter.security.JHHeaders;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

public class UnitTests {

  @Test
  public void testPlaywright() {
    try (Playwright pw = Playwright.create()) {
      Browser browser = pw.chromium().launch();
      System.out.println("Playwright: Chromium launched OK");
    }
  }

  @Test
  public void testHostExtraction(){
    String host = URI.create("https://www.linkedin.com/jobs/view/2345678901").getHost();
    if (host.startsWith("www.")) {
      host = host.substring(4);
    }
    System.out.println(host);
  }

  @Test
  public void testExpiredPages() {
    RestClient.Builder restBuilder = RestClient.builder();
    RestClient restClient = restBuilder
            .defaultHeader(JHHeaders.USER_AGENT,"Mozilla/5.0")
            .build();

    List<String> jobs = new ArrayList<>();

    jobs.add("https://allremote.jobs/remote-job/global-talents-hub-senior-java-engineer-36870af5-14a4-4aa2-b55c-d280439fb5e7");
    jobs.add("https://jobgether.com/offer/69098ffedec002a1d45b6d3d-senior-java-developer");
    jobs.add("https://twlglobalservicessrl.teamtailor.com/jobs/2370944-senior-java-developer-remote-opportunity");

    List<String> expiredKeywords = List.of("expired", "no longer exists");
    for (String jobURL : jobs) {
      HttpHeaders headers = new HttpHeaders();
      headers.set("User-Agent", "Mozilla/5.0");
      headers.setAccept(List.of(MediaType.TEXT_HTML));
      try {
        ResponseEntity<String> response = restClient
                .get()
                .uri(URI.create(jobURL))
                .accept(MediaType.TEXT_HTML)
                .retrieve()
                .toEntity(String.class);

        boolean wasRedirect = response.getStatusCode().is3xxRedirection() && response.getHeaders().getLocation() != null;
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null && !wasRedirect) {
          String html = response.getBody();
          boolean isExpired = false;

          for (String keyword : expiredKeywords) {
            Pattern pattern = Pattern.compile(">[A-Za-z0-9 .,!?\\-()]*" + Pattern.quote(keyword) + "[A-Za-z0-9 .,!?\\-()]*<",
                Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(html);

            if (matcher.find()) {
              isExpired = true;
              break;
            }
          }
          System.out.println("Expired: " + isExpired + " " + jobURL);
        } else {
          System.out.println("Expired: true " + jobURL);
        }
      } catch (Exception e) {
        System.out.println("Expired: true " + jobURL);
      }
    }
  }

}
