package org.pancakelab.http;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int WINDOW_SIZE_MS = 60_000; // 1 minute

    private final ConcurrentHashMap<String, RequestWindow> requestWindows = new ConcurrentHashMap<>();

    public boolean allowRequest(String clientIp) {
        RequestWindow window = requestWindows.compute(clientIp, (key, existing) -> {
            Instant now = Instant.now();
            if (existing == null || existing.isExpired(now)) {
                return new RequestWindow(now);
            }
            existing.incrementCount();
            return existing;
        });

        return window.getCount() <= MAX_REQUESTS_PER_MINUTE;
    }

    private static class RequestWindow {
        private final Instant startTime;
        private final AtomicInteger count;

        RequestWindow(Instant startTime) {
            this.startTime = startTime;
            this.count = new AtomicInteger(1);
        }

        boolean isExpired(Instant now) {
            return now.toEpochMilli() - startTime.toEpochMilli() >= WINDOW_SIZE_MS;
        }

        void incrementCount() {
            count.incrementAndGet();
        }

        int getCount() {
            return count.get();
        }
    }
}
