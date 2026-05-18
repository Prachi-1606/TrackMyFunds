package com.trackmyfunds.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory hourly rate limiter for outbound Gemini API calls.
 * <p>
 * The Gemini free tier on {@code gemini-1.5-flash} allows ~15 requests per
 * minute; we cap at 12 per hour app-wide to leave headroom for bursts and
 * stay friendly across many users on a single deployment.
 * <p>
 * State is intentionally in-process: a restart resets the counter, which is
 * fine for a single-instance personal-finance app. For multi-instance, swap
 * this for a Redis-backed counter.
 */
@Service
public class RateLimitService {

    public static final int HOURLY_LIMIT = 12;

    private final AtomicInteger callsThisWindow = new AtomicInteger(0);
    private volatile Instant    windowStart     = Instant.now();

    /**
     * Atomically increments the counter if there is room in the current window,
     * otherwise returns false. A new window opens automatically every 60 minutes
     * from {@link #windowStart}.
     *
     * @return true when the caller may proceed with the API call; false when the
     *         hourly budget is exhausted
     */
    public synchronized boolean tryAcquire() {
        rollWindowIfElapsed();
        if (callsThisWindow.get() >= HOURLY_LIMIT) {
            return false;
        }
        callsThisWindow.incrementAndGet();
        return true;
    }

    /** Number of calls used in the current hourly window (after rolling). */
    public synchronized int callsUsed() {
        rollWindowIfElapsed();
        return callsThisWindow.get();
    }

    private void rollWindowIfElapsed() {
        if (windowStart.plus(1, ChronoUnit.HOURS).isBefore(Instant.now())) {
            windowStart = Instant.now();
            callsThisWindow.set(0);
        }
    }
}
