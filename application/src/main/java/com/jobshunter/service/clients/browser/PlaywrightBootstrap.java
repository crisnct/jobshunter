package com.jobshunter.service.clients.browser;

import com.microsoft.playwright.Playwright;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public final class PlaywrightBootstrap {

  @PostConstruct
  public void installIfNeeded() {
    try (Playwright playwright = Playwright.create()) {
      // Intenționat gol:
      // forțează bootstrap-ul Playwright (Node + browsers)
    } catch (RuntimeException ex) {
      throw new IllegalStateException(
          "Failed to initialize Playwright runtime (Node/Chromium missing or incompatible)",
          ex
      );
    }
  }
}
