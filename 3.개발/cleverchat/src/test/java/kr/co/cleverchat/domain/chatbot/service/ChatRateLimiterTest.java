package kr.co.cleverchat.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

class ChatRateLimiterTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-28T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void memoryStoreKeepsDefaultThirtyRequestsPerMinuteBehavior() {
        ChatRateLimiter limiter = new ChatRateLimiter("memory", 30, 60, null, clock);

        for (int i = 0; i < 30; i++) {
            assertThat(limiter.isAllowed("anonymous:session")).isTrue();
        }

        assertThat(limiter.isAllowed("anonymous:session")).isFalse();
    }

    @Test
    void memoryStoreUsesConfiguredLimit() {
        ChatRateLimiter limiter = new ChatRateLimiter("memory", 2, 60, null, clock);

        assertThat(limiter.isAllowed("anonymous:session")).isTrue();
        assertThat(limiter.isAllowed("anonymous:session")).isTrue();
        assertThat(limiter.isAllowed("anonymous:session")).isFalse();
    }

    @Test
    void redisStoreBlocksWhenSortedSetCountReachesLimit() {
        StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zSet = org.mockito.Mockito.mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSet);
        when(zSet.zCard(anyString())).thenReturn(2L);
        ChatRateLimiter limiter = new ChatRateLimiter("redis", 2, 60, redisTemplate, clock);

        assertThat(limiter.isAllowed("anonymous:session")).isFalse();

        verify(zSet).removeRangeByScore(anyString(), eq(0.0), anyDouble());
        verify(zSet, never()).add(anyString(), anyString(), anyDouble());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void redisStoreAddsRequestWhenUnderLimit() {
        StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zSet = org.mockito.Mockito.mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSet);
        when(zSet.zCard(anyString())).thenReturn(1L);
        ChatRateLimiter limiter = new ChatRateLimiter("redis", 2, 60, redisTemplate, clock);

        assertThat(limiter.isAllowed("anonymous:session")).isTrue();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(zSet)
                .add(
                        keyCaptor.capture(),
                        anyString(),
                        eq((double) Instant.parse("2026-05-28T00:00:00Z").toEpochMilli()));
        assertThat(keyCaptor.getValue()).startsWith("cleverchat:chat:rate-limit:");
        assertThat(keyCaptor.getValue()).doesNotContain("anonymous", "session");
        verify(redisTemplate).expire(anyString(), eq(Duration.ofSeconds(61)));
    }

    @Test
    void redisStoreFailsOpenWhenRedisThrows() {
        StringRedisTemplate redisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
        when(redisTemplate.opsForZSet())
                .thenThrow(new IllegalStateException("redis down for anonymous:session"));
        ChatRateLimiter limiter = new ChatRateLimiter("redis", 2, 60, redisTemplate, clock);

        assertThat(limiter.isAllowed("anonymous:session")).isTrue();
    }
}
