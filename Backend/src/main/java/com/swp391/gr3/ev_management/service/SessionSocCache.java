package com.swp391.gr3.ev_management.service;

import org.springframework.stereotype.Component;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Optional;

@Component
public class SessionSocCache {

    private final Cache<Long, Integer> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(2))   // TTL 2 tiếng
            .maximumSize(10000)                      // tối đa 10k session
            .build();

    public void put(Long sessionId, int soc) {
        cache.put(sessionId, soc);
    }

    public Optional<Integer> get(Long sessionId) {
        return Optional.ofNullable(cache.getIfPresent(sessionId));
    }

    public void remove(Long sessionId) {
        cache.invalidate(sessionId);
    }
}
