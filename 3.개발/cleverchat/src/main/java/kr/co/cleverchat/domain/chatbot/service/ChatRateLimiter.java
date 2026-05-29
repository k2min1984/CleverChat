package kr.co.cleverchat.domain.chatbot.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(ChatRateLimiter.class);
    private static final String STORE_REDIS = "redis";

    private final Clock clock;
    private final String store;
    private final int maxRequests;
    private final Duration window;
    private final StringRedisTemplate redisTemplate;
    private final Map<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();

    @Autowired
    public ChatRateLimiter(
            @Value("${cleverchat.chat.rate-limit.store:memory}") String store,
            @Value("${cleverchat.chat.rate-limit.max-requests:30}") int maxRequests,
            @Value("${cleverchat.chat.rate-limit.window-seconds:60}") long windowSeconds,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this(
                store,
                maxRequests,
                windowSeconds,
                redisTemplateProvider.getIfAvailable(),
                Clock.systemUTC());
    }

    ChatRateLimiter(
            String store,
            int maxRequests,
            long windowSeconds,
            StringRedisTemplate redisTemplate,
            Clock clock) {
        this.clock = clock;
        this.store = store == null ? "memory" : store.trim().toLowerCase();
        this.maxRequests = Math.max(1, maxRequests);
        this.window = Duration.ofSeconds(Math.max(1, windowSeconds));
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String key) {
        if (STORE_REDIS.equals(store)) {
            return isRedisAllowed(key);
        }
        return isMemoryAllowed(key);
    }

    private boolean isMemoryAllowed(String key) {
        Instant now = Instant.now(clock);
        Deque<Instant> bucket =
                buckets.computeIfAbsent(hashedKey(key), ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            while (!bucket.isEmpty() && bucket.peekFirst().plus(window).isBefore(now)) {
                bucket.removeFirst();
            }
            if (bucket.size() >= maxRequests) {
                return false;
            }
            bucket.addLast(now);
            return true;
        }
    }

    private boolean isRedisAllowed(String key) {
        if (redisTemplate == null) {
            log.warn("RATE_LIMIT_REDIS_UNAVAILABLE reason=template_missing");
            return true;
        }
        try {
            Instant now = Instant.now(clock);
            String redisKey = "cleverchat:chat:rate-limit:" + hashedKey(key);
            double nowScore = now.toEpochMilli();
            double cutoffScore = now.minus(window).toEpochMilli();
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, cutoffScore);
            Long count = redisTemplate.opsForZSet().zCard(redisKey);
            if (count != null && count >= maxRequests) {
                return false;
            }
            redisTemplate
                    .opsForZSet()
                    .add(redisKey, now.toEpochMilli() + ":" + System.nanoTime(), nowScore);
            redisTemplate.expire(redisKey, window.plusSeconds(1));
            return true;
        } catch (Exception e) {
            log.warn("RATE_LIMIT_REDIS_UNAVAILABLE reason=operation_failed");
            return true;
        }
    }

    private String hashedKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(String.valueOf(key).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }
}
