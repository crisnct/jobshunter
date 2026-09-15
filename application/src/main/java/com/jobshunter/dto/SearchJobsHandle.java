package com.jobshunter.dto;

/**
 * Response of {@code POST /api/internal/search_jobs}: the search has been enqueued (one
 * {@link com.jobshunter.database.entities.JobOrderEntity} per configuration submitted, picked up
 * asynchronously by {@code JobHuntScheduler}), not completed yet.
 *
 * @param searchId opaque handle for {@code GET /api/internal/search_jobs/{searchId}}; encodes the
 *                 underlying job order ids.
 */
public record SearchJobsHandle(String searchId) {
}
