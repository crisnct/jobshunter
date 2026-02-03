package com.jobshunter.service.clients.browser;

import com.jobshunter.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Utility class about html body
 */
public final class HtmlUtils {

  private static final int MIN_BODY_LENGTH = 500;
  /**
   * SPA shells with root/app mount: often 10k–100k due to bundled scripts.
   */
  private static final int SPA_SHELL_MAX_LENGTH = 200_000;
  private static final int SPA_SCRIPT_RICH_MAX_LENGTH = 120_000;
  private static final int MIN_SCRIPT_TAGS_FOR_SPA = 4;

  /**
   * Framework/SPA markers that indicate client-side rendering.
   */
  private static final String[] SPA_MARKERS = {
      "createroot", "hydrateroot", "__next_data__", "ng-version=",
      "data-reactroot", "react-dom", "vue.runtime"
  };

  private HtmlUtils() {
  }

  /**
   * Heuristic to detect when an HTTP-fetched HTML body is likely incomplete or JS-rendered (SPA shell), so that we can fall back to Playwright for
   * full content.
   * <p>
   * Returns true if the body likely needs browser rendering (Playwright) to get meaningful content. Uses fast checks only (no full HTML parsing).
   */
  public static boolean needsPlaywrightRendering(String html) {
    if (html == null || html.isBlank()) {
      return true;
    }
    int len = html.length();
    if (len < MIN_BODY_LENGTH) {
      return true;
    }
    String lower = html.toLowerCase();
    boolean hasRootOrApp =
        lower.contains("id=\"root\"") || lower.contains("id='root'")
            || lower.contains("id=\"app\"") || lower.contains("id='app'");
    if (hasRootOrApp && len < SPA_SHELL_MAX_LENGTH) {
      return true;
    }
    for (String marker : SPA_MARKERS) {
      if (lower.contains(marker) && len < SPA_SHELL_MAX_LENGTH) {
        return true;
      }
    }
    int scriptCount = countSubstringOccurrences(lower, "<script");
    if (scriptCount >= MIN_SCRIPT_TAGS_FOR_SPA && len < SPA_SCRIPT_RICH_MAX_LENGTH) {
      return true;
    }
    return false;
  }

  private static int countSubstringOccurrences(String str, String sub) {
    int count = 0;
    int idx = 0;
    while ((idx = str.indexOf(sub, idx)) != -1) {
      count++;
      idx += sub.length();
    }
    return count;
  }

  public static String cleanupHTML(String html) {
    Document document = Jsoup.parse(html);
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
      if (p.text().length() > 800) {
        p.remove(); // long marketing prose
      }
    });

    String cleanedText = document.text()
        .replaceAll("\\s+", " ")
        .trim();

    if (cleanedText.length() > 7000) {
      cleanedText = cleanedText.substring(0, 7000);
    }
    cleanedText = StringUtils.removeDiacritics(cleanedText);
    return cleanedText;
  }

}
