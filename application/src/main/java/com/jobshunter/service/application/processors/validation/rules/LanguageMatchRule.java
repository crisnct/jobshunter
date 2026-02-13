package com.jobshunter.service.application.processors.validation.rules;

import com.jobshunter.service.application.processors.validation.ValidationContext;
import com.jobshunter.service.application.processors.validation.ValidationResult;
import com.jobshunter.service.application.processors.validation.ValidationRule;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * [Issue #46] Validation rule that filters job postings based on language requirements.
 * <p>
 * All known languages are loaded from the database and compiled into regex patterns.
 * The rule logic:
 * <ul>
 *   <li>If the user has no languages configured → accept the job (skip filter).</li>
 *   <li>If the job description does NOT mention any known language → accept the job.</li>
 *   <li>If the job description mentions languages and at least one matches the user's → accept.</li>
 *   <li>If the job description mentions languages but NONE match the user's → reject.</li>
 * </ul>
 * Example: user speaks French and Romanian. A job mentioning "English" is rejected.
 * A job mentioning "French" is accepted. A job with no language mentioned is accepted.
 */
@Slf4j
public class LanguageMatchRule implements ValidationRule {

  /** All known languages from the database, each with a compiled word-boundary regex. */
  private final List<LanguagePattern> allLanguagePatterns;

  /**
   * @param allLanguageNames all language names from the database (e.g. "English", "French", ...)
   */
  public LanguageMatchRule(List<String> allLanguageNames) {
    this.allLanguagePatterns = allLanguageNames.stream()
        .map(name -> new LanguagePattern(name, Pattern.compile("\\b" + Pattern.quote(name) + "\\b", Pattern.CASE_INSENSITIVE)))
        .toList();
  }

  @Override
  public ValidationResult validate(ValidationContext context) {
    List<String> userLanguages = context.getJobContext().getUserLanguages();

    // If the user hasn't declared any languages, skip this filter entirely
    if (userLanguages == null || userLanguages.isEmpty()) {
      return ValidationResult.success("No user languages specified, skipping language filter");
    }

    String html = context.getHtml();

    // Find all known languages mentioned in the job description
    List<String> mentionedLanguages = new ArrayList<>();
    for (LanguagePattern lp : allLanguagePatterns) {
      if (lp.pattern.matcher(html).find()) {
        mentionedLanguages.add(lp.name);
      }
    }

    // If the job doesn't mention any known language, accept it
    if (mentionedLanguages.isEmpty()) {
      return ValidationResult.success("No language requirements detected in job description");
    }

    // Check if at least one mentioned language matches the user's languages
    boolean hasMatch = mentionedLanguages.stream()
        .anyMatch(mentioned -> userLanguages.stream()
            .anyMatch(userLang -> userLang.equalsIgnoreCase(mentioned)));

    if (hasMatch) {
      return ValidationResult.success("Job mentions a language the user speaks: " + mentionedLanguages);
    }

    return ValidationResult.failure(
        "Job requires " + mentionedLanguages + " but user only speaks " + userLanguages);
  }

  @Override
  public String getRuleName() {
    return "LanguageMatchRule";
  }

  /** Holder for a language name and its compiled regex pattern. */
  private record LanguagePattern(String name, Pattern pattern) {}
}
