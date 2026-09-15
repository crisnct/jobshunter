package com.jobshunter.dto;

import java.util.List;

/**
 * Response of {@code GET /api/internal/search_jobs/{searchId}}.
 *
 * @param searchId       the polled search id, echoed back.
 * @param status         one of {@code "IN_PROGRESS"}, {@code "DONE"}, {@code "FAILED"}.
 * @param events         progress messages recorded so far, oldest first.
 * @param result         populated only when {@code status} is {@code "DONE"}.
 * @param errorMessage   populated only when {@code status} is {@code "FAILED"}.
 */
public record SearchJobSnapshotResponse(
    String searchId,
    String status,
    List<SearchStepEvent> events,
    SearchJobsResponse result,
    String errorMessage
) {
}
