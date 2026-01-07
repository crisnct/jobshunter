package com.jobshunter.dto;

import com.jobshunter.model.OrderStatus;
import java.time.Instant;

public record JobOrderResponse(
    Long id,
    Long engineConfigurationId,
    String provider,
    String model,
    boolean searchCompanies,
    boolean searchByPrompts,
    OrderStatus status,
    boolean notified,
    Instant modifiedAt,
    String errorMessage
) {

}
