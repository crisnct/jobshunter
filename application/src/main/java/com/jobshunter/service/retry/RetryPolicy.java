package com.jobshunter.service.retry;

import java.util.function.Predicate;

public record RetryPolicy<T>(
    String name,
    int maxAttempts,
    long delayMillis,
    Predicate<T> successCondition,
    Predicate<Throwable> retryOnException,
    T fallback
) {

}
