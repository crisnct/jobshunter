package com.jobshunter.service.application.hunting;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomInvalidReasons {

  private static final List<String> REASONS = List.of(
      // 1–3 (already present)
      "The URL does not point to a specific job posting page. It resolves to a generic careers page, job listings overview, category page, or a redirect.",
      "The job posting page does not display a clearly visible publication or last-updated date, so freshness (last 3 days) cannot be verified.",
      "The URL is not publicly accessible (requires login, returns an error, redirects unexpectedly, or the page cannot be reliably loaded).",

      // 4–10: URL & accessibility issues
      "The URL redirects multiple times and does not consistently resolve to a stable job posting page.",
      "The URL leads to a third-party tracking or referral page instead of the original job posting.",
      "The page returns an HTTP error status (4xx or 5xx).",
      "The page content cannot be loaded completely or times out.",
      "The URL points to a cached, archived, or snapshot version of a job posting.",
      "The URL format appears auto-generated and does not correspond to a real job posting.",
      "The URL points to a PDF or document instead of a web-based job posting page.",

      // 11–18: Date & freshness problems
      "The publication date is outside the allowed time window (older than 3 days).",
      "The page contains multiple dates and the publication or update date cannot be unambiguously determined.",
      "The page displays a relative date (e.g., 'posted recently') without a concrete calendar date.",
      "The publication date appears inconsistent with the page content or metadata.",
      "The date is present only in structured metadata but not visible on the page.",
      "The date is present only in page scripts or network calls and not human-visible.",
      "The posting appears recycled or re-posted without a clear update timestamp.",
      "The page indicates the role was published earlier and merely refreshed cosmetically.",

      // 19–27: Job status & validity
      "The job posting explicitly indicates that the position is no longer open.",
      "The job posting has been marked as filled or closed.",
      "The page content indicates an expired or inactive job listing.",
      "The role is labeled as 'talent pool', 'future opportunities', or similar non-active hiring status.",
      "The posting is a general hiring announcement without a concrete open role.",
      "The posting is an internal-only or referral-only position.",
      "The page content contradicts the existence of an active vacancy.",
      "The role is part of an ongoing campaign rather than a specific job opening.",
      "The posting describes a role that is no longer accepting applications.",

      // 28–35: Role mismatch & exclusions
      "The role is an internship, trainee, or entry-level program.",
      "The role is unpaid, volunteer-based, or otherwise not a paid position.",
      "The posting targets students or recent graduates only.",
      "The seniority level does not match the user’s experience (e.g., junior-only role).",
      "The role is unrelated to the user’s professional background.",
      "The job title is misleading and does not reflect the actual role described.",
      "The posting combines multiple unrelated roles into a single listing.",
      "The role is a temporary, seasonal, or short-term contract not requested by the user.",

      // 36–43: Location & work mode issues
      "The job location is not specified clearly on the posting page.",
      "The role is location-restricted and does not match the user’s location preferences.",
      "Remote eligibility is not explicitly stated on the job posting page.",
      "The posting explicitly excludes remote or hybrid work when required.",
      "The job requires relocation not mentioned or accepted by the user.",
      "The location information is contradictory or unclear.",
      "The role is limited to a specific country or region outside the user’s scope.",
      "The job requires on-site presence where remote work was expected.",

      // 44–50: Data quality & duplication
      "The posting is a duplicate of another job already included in the results.",
      "The same role appears multiple times under different URLs or tracking parameters.",
      "The company name cannot be reliably identified from the posting page.",
      "The job title cannot be clearly determined from the page content.",
      "The posting content is incomplete or truncated.",
      "The page content appears auto-generated or placeholder text.",
      "The posting lacks sufficient information to be considered a valid job listing."
  );

  private RandomInvalidReasons() {
    // utility class
  }

  public static String pick() {
    return REASONS.get(
        ThreadLocalRandom.current().nextInt(REASONS.size())
    );
  }
}
