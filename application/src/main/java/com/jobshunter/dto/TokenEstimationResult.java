package com.jobshunter.dto;

/**
 * Result of token estimation.
 */
public record TokenEstimationResult(

    int estimatedInputTokens,
    int estimatedToolTokens,
    int estimatedSchemaTokens,
    int estimatedOutputTokens,
    int estimatedTotalTokens,

    int safeContextLimit,
    boolean fitsContextWindow

) {}
