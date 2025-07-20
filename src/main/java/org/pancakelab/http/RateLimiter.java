package org.pancakelab.http;

import org.pancakelab.config.Configuration;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {
    private final int maxRequestsPerWindow;
    private final int windowSizeMs;
    private final ConcurrentHashMap<String, RequestWindow> requestWindows = new ConcurrentHashMap<>();

    public RateLimiter() {
        Configuration config = Configuration.getInstance();
        this.maxRequestsPerWindow = config.getRateLimitMaxRequests();
        this.windowSizeMs = config.getRateLimitWindowMs();
    }

    public boolean allowRequest(String clientIp) {
        RequestWindow window = requestWindows.compute(clientIp, (key, existing) -> {
            Instant now = Instant.now();
            if (existing == null || existing.isExpired(now)) {
                return new RequestWindow(now);
            }
            existing.incrementCount();
            return existing;
        });

        return window.getCount() <= maxRequestsPerWindow;
    }

    private class RequestWindow {
        private final Instant startTime;
        private final AtomicInteger count;

        RequestWindow(Instant startTime) {
            this.startTime = startTime;
            this.count = new AtomicInteger(1);
        }

        boolean isExpired(Instant now) {
            return now.toEpochMilli() - startTime.toEpochMilli() >= windowSizeMs;
        }

        void incrementCount() {
            count.incrementAndGet();
        }

        int getCount() {
            return count.get();
        }
    }
}
