package com.jobshunter.security.rateLimitBucket4J;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ViolationRegistry {

    private final ConcurrentHashMap<String, Integer> violations = new ConcurrentHashMap<>();

    public int increment(String key) {
        return violations.merge(key, 1, Integer::sum);
    }

    public void reset(String key) {
        violations.remove(key);
    }
}
