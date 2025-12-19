package com.jobshunter.service.snippets;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WeWorkRemotelyJsoupFixed {

  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
          "AppleWebKit/537.36 (KHTML, like Gecko) " +
          "Chrome/122.0.0.0 Safari/537.36";

  static void main() throws Exception {

    String url = "https://weworkremotely.com/categories/remote-programming-jobs";

    Document doc = Jsoup.connect(url)
        .userAgent(USER_AGENT)
        .timeout(15_000)
        .get();

    Elements links = doc.select("a.listing-link--unlocked[href^=/remote-jobs/]");

    System.out.println("Job links found: " + links.size());

    int printed = 0;
    for (Element link : links) {

      String jobUrl = "https://weworkremotely.com" + link.attr("href");

      // titlul jobului
      String title = link.selectFirst("span.listing-title") != null
          ? link.selectFirst("span.listing-title").text()
          : link.text(); // fallback

      // compania
      String company = link.selectFirst("span.listing-company") != null
          ? link.selectFirst("span.listing-company").text()
          : "";

      if (title.isBlank()) {
        continue;
      }

      System.out.println(title + " | " + company + " | " + jobUrl);

      printed++;
      if (printed >= 5) {
        break;
      }
    }
  }
}
