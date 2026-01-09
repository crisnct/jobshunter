package com.jobshunter.service.clients.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PreDestroy;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Manages a single Playwright instance and a single Browser instance
 * for the entire JVM lifecycle.
 *
 * Design goals:
 * - visible browser (headed mode)
 * - stable lifecycle
 * - safe for limited, serialized usage
 * - good debugging experience
 */
@Component
public final class PlaywrightManager {

  private final Playwright playwright;

  private final Browser browser;

  public PlaywrightManager() {
    this.playwright = Playwright.create();
    this.browser =
        playwright.chromium().launch(
            new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of(
                    "--disable-blink-features=AutomationControlled",
                    "--start-maximized",
                    "--disable-http2",
                    "--disable-quic"
                ))
        );
  }

  /**
   * Creates a new isolated browser context.
   * Must be called in a controlled (serialized) manner.
   */
  public synchronized BrowserContext newContext() {
    return browser.newContext(
        new Browser.NewContextOptions()
            .setUserAgent(RandomBrowserUserAgent.pick())
            .setTimezoneId("Europe/Bucharest")
            .setViewportSize(1366, 768)
            .setLocale("en-US")
    );
  }

  /**
   * Clean shutdown at JVM exit.
   * Must not be called while contexts/pages are still in use.
   */
  @PreDestroy
  public void shutdown() {
    try {
      browser.close();
    } finally {
      playwright.close();
    }
  }
}
