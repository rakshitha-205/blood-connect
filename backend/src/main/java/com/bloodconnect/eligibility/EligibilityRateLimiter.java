package com.bloodconnect.eligibility;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Limits checks per IP address, because each check can call the paid AI model. */
@Component
public class EligibilityRateLimiter {

    private static final Duration WINDOW = Duration.ofHours(1);

    private final int maxChecksPerHour;
    private final Map<String, Deque<Instant>> checks = new ConcurrentHashMap<>();

    public EligibilityRateLimiter(@Value("${bloodconnect.eligibility.max-checks-per-hour:10}") int maxChecksPerHour) {
        this.maxChecksPerHour = maxChecksPerHour;
    }

    public void acquire(String clientAddress) {
        Instant now = Instant.now();
        Deque<Instant> recent = checks.computeIfAbsent(clientAddress, key -> new ArrayDeque<>());
        synchronized (recent) {
            while (!recent.isEmpty() && recent.peekFirst().isBefore(now.minus(WINDOW))) {
                recent.pollFirst();
            }
            if (recent.size() >= maxChecksPerHour) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "You've checked your eligibility many times in the last hour. Please try again later.");
            }
            recent.addLast(now);
        }
    }
}
