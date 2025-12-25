package com.jobshunter.model;

import jakarta.validation.constraints.NotNull;

public record EngineSelection(
    @NotNull EngineType type,
    @NotNull EngineTier tier
) {
}

