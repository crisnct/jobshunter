package com.jobshunter.service.application.processors.validation.rules;

import com.jobshunter.service.application.processors.validation.ValidationContext;
import com.jobshunter.service.application.processors.validation.ValidationResult;
import com.jobshunter.service.application.processors.validation.ValidationRule;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * [Issue #46] Validation rule that checks whether a job posting's language requirements
 * match the user's declared languages.
 * <p>
 * If the job HTML mentions a known language (e.g. "english", "french") and the user
 * has NOT declared that language in their profile, the job is rejected.
 * If the user has no languages configured, the rule is skipped (passes).
 */
@Slf4j
public class LanguageMatchRule implements ValidationRule {

  /** Pairs of (language name, compiled word-boundary pattern) for each known language. */
  private final List<LanguagePattern> languagePatterns;

  public LanguageMatchRule(List<String> languageExpressions) {
    this.languagePatterns = languageExpressions.stream()
        .map(expr -> new LanguagePattern(expr, Pattern.compile("\\b" + expr + "\\b", Pattern.CASE_INSENSITIVE)))
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

    // For each known language, check if the job page mentions it
    for (LanguagePattern lp : languagePatterns) {
      if (lp.pattern.matcher(html).find()) {
        // The job mentions this language — verify the user speaks it
        boolean userSpeaksIt = userLanguages.stream()
            .anyMatch(userLang -> userLang.equalsIgnoreCase(lp.name));
        if (!userSpeaksIt) {
          return ValidationResult.failure(
              "Job requires '" + lp.name + "' which is not in user's languages: " + userLanguages);
        }
      }
    }
    return ValidationResult.success("Language requirements match user preferences");
  }

  @Override
  public String getRuleName() {
    return "LanguageMatchRule";
  }

  /** Simple holder for a language name and its compiled regex pattern. */
  private record LanguagePattern(String name, Pattern pattern) {}
}
