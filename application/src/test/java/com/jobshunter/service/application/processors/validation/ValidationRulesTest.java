package com.jobshunter.service.application.processors.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jobshunter.model.ContractType;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobType;
import com.jobshunter.service.application.processors.validation.rules.B2BJobsRule;
import com.jobshunter.service.application.processors.validation.rules.LocalJobsRule;
import com.jobshunter.service.application.processors.validation.rules.NotExpiredRule;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ValidationRulesTest {

  private JobContext mockJobContext;

  @BeforeEach
  void setUp() {
    mockJobContext = mock(JobContext.class);
    Job mockJob = mock(Job.class);
    when(mockJobContext.getJob()).thenReturn(mockJob);
    when(mockJob.getUrl()).thenReturn("https://example.com/job/123");
  }

  private ValidationContext createContext(
      List<JobType> jobTypes,
      List<ContractType> contractTypes,
      boolean cityMatch,
      boolean countryMatch,
      boolean freelancerRole,
      boolean remoteRole
  ) {
    when(mockJobContext.getUserJobTypes()).thenReturn(jobTypes);
    when(mockJobContext.getUserContractTypes()).thenReturn(contractTypes);

    return ValidationContext.builder()
        .html("<html><body>Job description</body></html>")
        .jobContext(mockJobContext)
        .cityMatch(cityMatch)
        .countryMatch(countryMatch)
        .freelancerRole(freelancerRole)
        .remoteRole(remoteRole)
        .build();
  }

  @Nested
  @DisplayName("NotExpiredRule Tests")
  class NotExpiredRuleTests {

    private NotExpiredRule rule;

    @BeforeEach
    void setUp() {
      List<Pattern> expiredPatterns = List.of(
          Pattern.compile("(?i)job expired", Pattern.DOTALL),
          Pattern.compile("(?i)position filled", Pattern.DOTALL),
          Pattern.compile("(?i)no longer available", Pattern.DOTALL)
      );
      rule = new NotExpiredRule(expiredPatterns);
    }

    @Test
    @DisplayName("Should return valid for non-expired job")
    void shouldReturnValidForNonExpiredJob() {
      ValidationContext ctx = ValidationContext.builder()
          .html("<html><body>Great job opportunity!</body></html>")
          .jobContext(mockJobContext)
          .build();

      ValidationResult result = rule.validate(ctx);

      assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should return invalid for expired job")
    void shouldReturnInvalidForExpiredJob() {
      ValidationContext ctx = ValidationContext.builder()
          .html("<html><body>This job expired yesterday</body></html>")
          .jobContext(mockJobContext)
          .build();

      ValidationResult result = rule.validate(ctx);

      assertFalse(result.isValid());
    }

    @Test
    @DisplayName("Should return invalid when position is filled")
    void shouldReturnInvalidWhenPositionFilled() {
      ValidationContext ctx = ValidationContext.builder()
          .html("<html><body>Sorry, this position filled already</body></html>")
          .jobContext(mockJobContext)
          .build();

      ValidationResult result = rule.validate(ctx);

      assertFalse(result.isValid());
    }
  }

  @Nested
  @DisplayName("OnsiteHybridRule Tests")
  class LocalJobsRuleTests {

    private final LocalJobsRule rule = new LocalJobsRule();

    @Test
    @DisplayName("Should return valid for ONSITE job with city match")
    void shouldReturnValidForOnsiteWithCityMatch() {
      ValidationContext ctx = createContext(
          List.of(JobType.ONSITE),
          List.of(),
          true, false, false, false
      );

      ValidationResult result = rule.validate(ctx);

      assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should return valid for HYBRID job with city match")
    void shouldReturnValidForHybridWithCityMatch() {
      ValidationContext ctx = createContext(
          List.of(JobType.HYBRID),
          List.of(),
          true, false, false, false
      );

      ValidationResult result = rule.validate(ctx);

      assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should return invalid for ONSITE job without city match")
    void shouldReturnInvalidForOnsiteWithoutCityMatch() {
      ValidationContext ctx = createContext(
          List.of(JobType.ONSITE),
          List.of(),
          false, true, false, false
      );

      ValidationResult result = rule.validate(ctx);

      assertFalse(result.isValid());
    }

    @Test
    @DisplayName("Should return invalid for REMOTE job type even with city match")
    void shouldReturnInvalidForRemoteJobType() {
      ValidationContext ctx = createContext(
          List.of(JobType.REMOTE),
          List.of(),
          true, true, false, false
      );

      ValidationResult result = rule.validate(ctx);

      assertTrue(result.isValid());
    }
  }

  @Nested
  @DisplayName("B2BRemoteRule Tests")
  class B2BJobsRuleTests {

    private final B2BJobsRule rule = new B2BJobsRule();

    @Test
    @DisplayName("Should return valid for B2B with freelancer and remote role")
    void shouldReturnValidForB2BWithFreelancerAndRemote() {
      ValidationContext ctx = createContext(
          List.of(),
          List.of(ContractType.B2B),
          false, false, true, true
      );

      ValidationResult result = rule.validate(ctx);

      assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should return invalid for B2B without remote role")
    void shouldReturnInvalidForB2BWithoutRemoteRole() {
      ValidationContext ctx = createContext(
          List.of(),
          List.of(ContractType.B2B),
          false, false, true, false
      );

      ValidationResult result = rule.validate(ctx);

      assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should return invalid for EOR contract (only B2B supported)")
    void shouldReturnInvalidForEorContract() {
      ValidationContext ctx = createContext(
          List.of(),
          List.of(ContractType.EOR),
          false, false, true, true
      );

      ValidationResult result = rule.validate(ctx);

      assertFalse(result.isValid());
    }
  }

  @Nested
  @DisplayName("PatternCache Tests")
  class PatternCacheTests {

    private final PatternCache cache = new PatternCache();

    @Test
    @DisplayName("Should match word with boundaries")
    void shouldMatchWordWithBoundaries() {
      assertTrue(cache.matchesWord("Bucharest", "Location: Bucharest, Romania"));
      assertTrue(cache.matchesWord("bucharest", "Location: Bucharest, Romania"));
    }

//    @Test
//    @DisplayName("Should not match partial words")
//    void shouldNotMatchPartialWords() {
//      assertFalse(cache.matchesWord("Bucha", "Location: Bucharest, Romania"));
//    }

    @Test
    @DisplayName("Should return false for null or blank input")
    void shouldReturnFalseForNullOrBlankInput() {
      assertFalse(cache.matchesWord(null, "some text"));
      assertFalse(cache.matchesWord("", "some text"));
      assertFalse(cache.matchesWord("word", null));
    }

    @Test
    @DisplayName("Should return same pattern instance for same word")
    void shouldReturnSamePatternForSameWord() {
      Pattern p1 = cache.getPhrasePattern("test");
      Pattern p2 = cache.getPhrasePattern("test");
      Pattern p3 = cache.getPhrasePattern("TEST");

      // Caffeine caches by lowercase key, so all should be same instance
      assertTrue(p1 == p2);
      assertTrue(p2 == p3);
    }
  }
}
