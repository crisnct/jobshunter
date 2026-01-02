package com.jobshunter.dto;

import com.jobshunter.model.OrderStatus;
import java.time.Instant;

public record JobOrderResponse(
    Long id,
    Long engineConfigurationId,
    String engine,
    String model,
    boolean searchCompanies,
    OrderStatus status,
    boolean notified,
    Instant timestamp,
    String errorMessage
) {
}
