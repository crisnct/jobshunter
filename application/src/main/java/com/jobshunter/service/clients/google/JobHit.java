package com.jobshunter.service.clients.google;

import java.util.List;

public record JobHit(
    String title,
    String company,
    String location,
    String shareLink,
    String jobId,
    List<String> applyLinks
) {

}