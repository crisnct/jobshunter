package com.jobshunter.service.clients.perplexity;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.dto.perplexityResponse.Annotation;
import com.jobshunter.dto.perplexityResponse.ContentItem;
import com.jobshunter.dto.perplexityResponse.OutputItem;
import com.jobshunter.dto.perplexityResponse.PerplexityResponse;
import com.jobshunter.dto.perplexityResponse.SearchResult;
import com.jobshunter.model.Job;
import com.jobshunter.model.UrlVerificationMode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class PerplexityUrlVerifierTest {

  private static final String SEEN = "https://boards.greenhouse.io/acme/jobs/123";
  private static final String SAME_HOST_OTHER_JOB = "https://boards.greenhouse.io/acme/jobs/999";
  private static final String INVENTED = "https://invented.example.org/jobs/42";

  private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

  private PerplexityUrlVerifier verifier(UrlVerificationMode mode) {
    ApplicationProperties properties = new ApplicationProperties();
    properties.getPerplexity().setUrlVerification(mode);
    return new PerplexityUrlVerifier(properties, meters);
  }

  private static PerplexityResponse responseSeeing(String... urls) {
    List<SearchResult> results = java.util.Arrays.stream(urls).map(u -> new SearchResult(u, "t", "s", null, null)).toList();
    OutputItem search = new OutputItem(null, OutputItem.TYPE_SEARCH_RESULTS, null, null, List.of("q"), results, null);
    return new PerplexityResponse("resp_1", "completed", "m", List.of(search), null, null);
  }

  private static List<Job> jobs(String... urls) {
    return java.util.Arrays.stream(urls).map(Job::new).toList();
  }

  @Test
  void strictKeepsOnlyUrlsSeenVerbatim() {
    List<Job> kept = verifier(UrlVerificationMode.STRICT)
        .verify(jobs(SEEN, SAME_HOST_OTHER_JOB, INVENTED), responseSeeing(SEEN));

    assertThat(kept).extracting(Job::getUrl).containsExactly(SEEN);
  }

  @Test
  void hostModeKeepsUrlsOfKnownHostsAndDropsInventedOnes() {
    List<Job> kept = verifier(UrlVerificationMode.HOST)
        .verify(jobs(SEEN, SAME_HOST_OTHER_JOB, INVENTED), responseSeeing(SEEN));

    assertThat(kept).extracting(Job::getUrl).containsExactly(SEEN, SAME_HOST_OTHER_JOB);
  }

  @Test
  void offKeepsEverythingButStillCountsWhatWouldBeRejected() {
    List<Job> kept = verifier(UrlVerificationMode.OFF).verify(jobs(SEEN, INVENTED), responseSeeing(SEEN));

    assertThat(kept).hasSize(2);
    assertThat(meters.counter(PerplexityUrlVerifier.REJECTED_METRIC, "mode", "OFF").count()).isEqualTo(1);
  }

  @Test
  void rejectionsAreCountedPerMode() {
    verifier(UrlVerificationMode.HOST).verify(jobs(INVENTED, "https://other.example.org/x"), responseSeeing(SEEN));

    assertThat(meters.counter(PerplexityUrlVerifier.REJECTED_METRIC, "mode", "HOST").count()).isEqualTo(2);
  }

  @Test
  void ignoresSchemeWwwFragmentAndTrailingSlash() {
    List<Job> kept = verifier(UrlVerificationMode.STRICT)
        .verify(jobs("http://www.boards.greenhouse.io/acme/jobs/123/#apply"), responseSeeing(SEEN));

    assertThat(kept).hasSize(1);
  }

  @Test
  void queryStringIsPartOfTheStrictKey() {
    List<Job> kept = verifier(UrlVerificationMode.STRICT)
        .verify(jobs("https://boards.greenhouse.io/acme/jobs/123?gh_jid=1"), responseSeeing(SEEN));

    assertThat(kept).isEmpty();
  }

  @Test
  void annotationsAndFetchedPagesCountAsEvidence() {
    OutputItem fetched = new OutputItem(null, OutputItem.TYPE_FETCH_URL_RESULTS, null, null, null, null,
        List.of(new SearchResult("https://careers.acme.com/jobs", "t", "s", null, null)));
    ContentItem text = new ContentItem(ContentItem.TYPE_OUTPUT_TEXT, "{}",
        List.of(new Annotation("url_citation", "https://jobs.lever.co/acme/1", "t")));
    OutputItem message = new OutputItem(null, OutputItem.TYPE_MESSAGE, null, List.of(text), null, null, null);
    PerplexityResponse response = new PerplexityResponse("resp_1", "completed", "m", List.of(fetched, message), null, null);

    List<Job> kept = verifier(UrlVerificationMode.STRICT)
        .verify(jobs("https://careers.acme.com/jobs", "https://jobs.lever.co/acme/1", INVENTED), response);

    assertThat(kept).extracting(Job::getUrl).containsExactly("https://careers.acme.com/jobs", "https://jobs.lever.co/acme/1");
  }

  @Test
  void failsOpenWhenTheResponseHasNoEvidence() {
    PerplexityResponse noEvidence = new PerplexityResponse("resp_1", "completed", "m", List.of(), null, null);

    List<Job> kept = verifier(UrlVerificationMode.STRICT).verify(jobs(SEEN, INVENTED), noEvidence);

    assertThat(kept).hasSize(2);
    assertThat(meters.getMeters()).isEmpty();
  }

  @Test
  void malformedUrlsAreRejected() {
    List<Job> kept = verifier(UrlVerificationMode.HOST).verify(jobs("not a url", "ftp://boards.greenhouse.io/x"), responseSeeing(SEEN));

    assertThat(kept).isEmpty();
  }

  @Test
  void emptyInputIsReturnedUntouched() {
    assertThat(verifier(UrlVerificationMode.STRICT).verify(List.of(), responseSeeing(SEEN))).isEmpty();
  }
}
