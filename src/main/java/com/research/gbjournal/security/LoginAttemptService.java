package com.research.gbjournal.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory throttling for credential endpoints.
 *
 * Keys are account emails rather than client IPs: every browser login is relayed through the
 * Next.js server, so the backend sees the same IP for all users.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_FAILED_LOGINS = 10;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);

    private static final int MAX_RESET_REQUESTS = 3;
    private static final Duration RESET_WINDOW = Duration.ofMinutes(15);

    private static final int MAX_TRACKED_KEYS = 50_000;

    private final Map<String, Deque<Instant>> failedLogins = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> resetRequests = new ConcurrentHashMap<>();

    public boolean isLoginBlocked(String email) {
        return countRecent(failedLogins, key(email), LOGIN_WINDOW) >= MAX_FAILED_LOGINS;
    }

    public void recordLoginFailure(String email) {
        record(failedLogins, key(email), LOGIN_WINDOW);
    }

    public void recordLoginSuccess(String email) {
        failedLogins.remove(key(email));
    }

    /**
     * Records a password-reset request and returns false when the email has already
     * requested too many resets in the current window (prevents inbox flooding).
     */
    public boolean tryPasswordReset(String email) {
        String k = key(email);
        if (countRecent(resetRequests, k, RESET_WINDOW) >= MAX_RESET_REQUESTS) {
            return false;
        }
        record(resetRequests, k, RESET_WINDOW);
        return true;
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static int countRecent(Map<String, Deque<Instant>> map, String key, Duration window) {
        Deque<Instant> attempts = map.get(key);
        if (attempts == null) {
            return 0;
        }
        synchronized (attempts) {
            prune(attempts, window);
            return attempts.size();
        }
    }

    private static void record(Map<String, Deque<Instant>> map, String key, Duration window) {
        if (map.size() > MAX_TRACKED_KEYS) {
            // Bound memory under a credential-stuffing burst; stale entries are simply dropped.
            map.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    prune(e.getValue(), window);
                    return e.getValue().isEmpty();
                }
            });
        }
        Deque<Instant> attempts = map.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts, window);
            attempts.addLast(Instant.now());
        }
    }

    private static void prune(Deque<Instant> attempts, Duration window) {
        Instant cutoff = Instant.now().minus(window);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
            attempts.pollFirst();
        }
    }
}
