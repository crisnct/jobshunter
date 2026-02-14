package com.jobshunter.service.clients.browser;

import com.jobshunter.config.ApplicationProperties;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Manages a single Playwright instance, a single Browser instance, and a fixed pool
 * of reusable {@link BrowserContext} objects for the entire JVM lifecycle.
 * <p>
 * Contexts are pre-created at startup and recycled via {@link #borrowContext()} /
 * {@link #returnContext(BrowserContext)}.  They are never closed between requests;
 * only pages are closed after each use.  The pool size naturally caps concurrency.
 */
@Slf4j
@Component
public final class PlaywrightManager {

  private final int poolSize;

  private Playwright playwright;
  private Browser browser;

  private final BlockingQueue<BrowserContext> contextPool = new LinkedBlockingQueue<>();

  public PlaywrightManager(ApplicationProperties properties) {
    this.poolSize = properties.getJobsHunter().getThreads().getPlaywrightContextPoolSize();
  }

  @PostConstruct
  public void init() {
    this.playwright = Playwright.create();
    this.browser = playwright.chromium().launch(
        new BrowserType.LaunchOptions()
            .setHeadless(true)
            .setArgs(List.of(
                "--disable-blink-features=AutomationControlled",
                "--start-maximized",
                "--disable-http2",
                "--disable-quic"
            ))
    );

    for (int i = 0; i < poolSize; i++) {
      contextPool.add(createContext());
    }
    log.info("Playwright context pool initialized with {} contexts", poolSize);
  }

  private BrowserContext createContext() {
    return browser.newContext(
        new Browser.NewContextOptions()
            .setUserAgent(RandomBrowserUserAgent.pick())
            .setTimezoneId("Europe/Bucharest")
            .setViewportSize(1366, 768)
            .setLocale("en-US")
    );
  }

  /**
   * Borrows a context from the pool, blocking if none is available.
   */
  public BrowserContext borrowContext() throws InterruptedException {
    return contextPool.take();
  }

  /**
   * Returns a context to the pool after closing all its open pages.
   */
  public void returnContext(BrowserContext context) {
    try {
      for (Page page : context.pages()) {
        page.close();
      }
    } catch (Exception e) {
      log.warn("Error closing pages before returning context to pool", e);
    }
    contextPool.offer(context);
  }

  /**
   * Clean shutdown at JVM exit.  Drains and closes all pooled contexts,
   * then closes the browser and Playwright instance.
   */
  @PreDestroy
  public void shutdown() {
    log.info("Shutting down Playwright context pool ({} contexts)...", contextPool.size());
    List<BrowserContext> remaining = new ArrayList<>();
    contextPool.drainTo(remaining);
    for (BrowserContext ctx : remaining) {
      try {
        ctx.close();
      } catch (Exception e) {
        log.warn("Error closing context during shutdown", e);
      }
    }
    try {
      browser.close();
    } finally {
      playwright.close();
    }
    log.info("Playwright shut down successfully");
  }
}
