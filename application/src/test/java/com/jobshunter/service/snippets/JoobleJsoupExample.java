package com.jobshunter.service.snippets;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class JoobleJsoupExample {

  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
      "AppleWebKit/537.36 (KHTML, like Gecko) " +
      "Chrome/122.0.0.0 Safari/537.36";

  public static void main(String[] args) throws Exception {

    String url = "https://jooble.org/SearchResult?keywords=java&location=Romania";

    Document doc = Jsoup.connect(url)
        .userAgent(USER_AGENT)
        .timeout(15_000)
        .get();

    // 🔑 cheia: fiecare job este un <article>
    Elements jobs = doc.select("article");

    System.out.println("Articles found: " + jobs.size());

    int printed = 0;
    for (Element job : jobs) {

      Element linkEl = job.selectFirst("a[href]");
      if (linkEl == null) {
        continue;
      }

      String jobUrl = linkEl.absUrl("href");
      if (jobUrl.isBlank()) {
        continue;
      }

      String title = linkEl.text().trim();

      Element companyEl = job.selectFirst("span.company");
      String company = companyEl != null ? companyEl.text().trim() : "";

      Element locationEl = job.selectFirst("span.location");
      String location = locationEl != null ? locationEl.text().trim() : "";

      if (title.isBlank()) {
        continue;
      }

      System.out.println(title + " | " + company + " | " + location);
      System.out.println(" → " + jobUrl);

      printed++;
      if (printed >= 5) {
        break;
      }
    }
  }
}
