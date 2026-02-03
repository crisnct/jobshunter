package com.jobshunter;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Utility class for string operations used by AI external system clients.
 */
public final class StringUtils extends org.apache.commons.lang3.StringUtils {

  private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

  /**
   * Removes diacritical marks (accents) from a string. For example: "Café" becomes "Cafe", "Ștefan" becomes "Stefan".
   *
   * @param input The input string to process
   * @return The string without diacritical marks, or the original string if null or blank
   */
  public static String removeDiacritics(String input) {
    if (input == null || input.isBlank()) {
      return input;
    }

    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    return DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
  }
}