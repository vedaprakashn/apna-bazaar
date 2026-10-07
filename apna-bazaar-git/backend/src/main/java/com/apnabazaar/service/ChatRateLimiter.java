package com.apnabazaar.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.function.LongSupplier;
import java.util.concurrent.TimeUnit;

@Component
public class ChatRateLimiter {
    private static final long WINDOW_MS = 60_000;
    private static final int MAX_CLIENTS = 10_000;
    private final int limit;
    private final LongSupplier clock;
    private final Map<String, Deque<Long>> windows = new HashMap<>();
    public record Decision(boolean allowed, long retryAfterSeconds) {}

    @Autowired
    public ChatRateLimiter(@Value("${apna.chat.messages-per-minute:10}") int limit) {
        this(limit, () -> TimeUnit.NANOSECONDS.toMillis(System.nanoTime()));
    }
    public ChatRateLimiter(int limit, LongSupplier clock) {
        if(limit < 1) throw new IllegalArgumentException("Message limit must be positive");
        this.limit = limit;
        this.clock = clock;
    }
    public synchronized Decision acquire(String client) {
        long now = clock.getAsLong();
        var window = windows.get(client);
        if(window == null) {
            if(windows.size() >= MAX_CLIENTS) cleanup();
            if(windows.size() >= MAX_CLIENTS) return new Decision(false,60);
            window = new ArrayDeque<>();
            windows.put(client,window);
        }
        while(!window.isEmpty() && now-window.getFirst() >= WINDOW_MS) window.removeFirst();
        if(window.size() >= limit) return new Decision(false,Math.max(1,(WINDOW_MS-(now-window.getFirst())+999)/1000));
        window.addLast(now);
        return new Decision(true,0);
    }
    @Scheduled(fixedDelay=60_000)
    public synchronized void cleanup() {
        long now=clock.getAsLong();
        windows.entrySet().removeIf(e -> e.getValue().isEmpty() || now-e.getValue().getLast() >= WINDOW_MS);
    }
}
