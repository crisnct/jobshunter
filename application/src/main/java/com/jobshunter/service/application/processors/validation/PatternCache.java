package com.jobshunter.service.application.processors.validation;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Cache for compiled regex Pattern objects to avoid repeated compilation. Thread-safe and uses Caffeine for efficient caching.
 */
@Component
public class PatternCache {

  private static final int MAX_CACHE_SIZE = 500;
  private static final Duration EXPIRE_AFTER_ACCESS = Duration.ofHours(1);

  private final Cache<String, Pattern> patternCache;

  public PatternCache() {
    this.patternCache = Caffeine.newBuilder()
        .maximumSize(MAX_CACHE_SIZE)
        .expireAfterAccess(EXPIRE_AFTER_ACCESS)
        .build();
  }

  /**
   * Gets or creates a word boundary pattern for the given value. The pattern matches the value surrounded by non-alphanumeric characters.
   *
   * @param value the word to create a pattern for
   * @return a compiled Pattern that matches the word with boundaries
   */
  public Pattern getPhrasePattern(String value) {
    String key = value.toLowerCase();
    return patternCache.get(key, this::buildPhrasePattern);
  }

  private Pattern buildPhrasePattern(String value) {
    String escaped = value.trim()
        .replaceAll("([\\\\.^$|?*+()\\[\\]{}])", "\\\\$1") // escape regex meta
        .replaceAll("\\s+", "\\\\s+");                    // whitespace flexibil

    String regex = "(?<!\\p{L}\\p{N})" + escaped + "(?!\\p{L}\\p{N})";

    return Pattern.compile(
        regex,
        Pattern.CASE_INSENSITIVE
            | Pattern.UNICODE_CASE
            | Pattern.UNICODE_CHARACTER_CLASS
    );
  }

  /**
   * Checks if the given text matches the word pattern for the specified value.
   *
   * @param value the word to search for
   * @param text  the text to search in
   * @return true if the word is found in the text
   */
  public boolean matchesWord(String value, String text) {
    if (value == null || value.isBlank() || text == null) {
      return false;
    }
    return getPhrasePattern(value).matcher(text).find();
  }
}
