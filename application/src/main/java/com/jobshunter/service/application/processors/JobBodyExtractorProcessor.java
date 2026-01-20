package com.jobshunter.service.application.processors;

import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobPhase;
import com.jobshunter.service.clients.browser.HttpFetchResult;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class JobBodyExtractorProcessor implements JobProcessor {

  @Override
  public JobContext processAsync(JobContext context) {
    HttpFetchResult result = context.getFetchResult();
    if (context.hasFetchResult()) {
      context.setBody(this.cleanupHTML(result.body()));
      context.setPhase(JobPhase.BODY_EXTRACTION);
    } else {
      context.failJob("Missing body from fetch result");
    }
    return context;
  }

  private String cleanupHTML(String body) {
    Document document = Jsoup.parse(body);
    document.select("script, style, nav, footer, header, aside").remove();
    document.select("button, a").remove();
    String[] noisePhrases = {
        "about us", "who we are", "why join", "benefits",
        "equal opportunity", "application process",
        "how to apply", "we offer", "our culture",
        "diversity", "values"
    };
    document.select("p, li").forEach(el -> {
      String text = el.text().toLowerCase();
      for (String phrase : noisePhrases) {
        if (text.contains(phrase)) {
          el.remove();
          break;
        }
      }
    });
    document.select("p").forEach(p -> {
      if (p.text().length() > 400) {
        p.remove(); // long marketing prose
      }
    });

    String cleanedText = document.text()
        .replaceAll("\\s+", " ")
        .trim();

    if (cleanedText.length() > 4000) {
      cleanedText = cleanedText.substring(0, 4000);
    }
    return cleanedText;
  }

}
