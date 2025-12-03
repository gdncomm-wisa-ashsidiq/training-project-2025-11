package com.gdn.training.api_gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void blacklistStoresKeyWhenTtlPositive() {
        Duration ttl = Duration.ofMinutes(5);

        tokenBlacklistService.blacklist("jwt-123", ttl);

        verify(valueOperations).set(eq("jwt:blacklist:jwt-123"), eq("1"), eq(ttl));
    }

    @Test
    void blacklistDoesNothingWhenInputInvalid() {
        tokenBlacklistService.blacklist("", Duration.ofMinutes(5));
        tokenBlacklistService.blacklist("id", Duration.ZERO);

        verify(valueOperations, never()).set(any(), any(), any());
    }

    @Test
    void isBlacklistedReturnsTrueWhenKeyExists() {
        when(redisTemplate.hasKey("jwt:blacklist:jwt-1")).thenReturn(true);

        assertThat(tokenBlacklistService.isBlacklisted("jwt-1")).isTrue();
    }

    @Test
    void isBlacklistedReturnsFalseWhenKeyMissing() {
        when(redisTemplate.hasKey("jwt:blacklist:jwt-2")).thenReturn(false);

        assertThat(tokenBlacklistService.isBlacklisted("jwt-2")).isFalse();
    }
}

