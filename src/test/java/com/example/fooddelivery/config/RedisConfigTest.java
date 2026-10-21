package com.example.fooddelivery.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("RedisConfig Error Handler Unit Tests")
class RedisConfigTest {

    @Test
    @DisplayName("CacheErrorHandler gracefully catches Redis exceptions without propagating them")
    void testCacheErrorHandlerDoesNotThrow() {
        RedisConfig config = new RedisConfig();
        CacheErrorHandler errorHandler = config.errorHandler();

        Cache mockCache = mock(Cache.class);
        when(mockCache.getName()).thenReturn("testCache");

        RuntimeException redisException = new RuntimeException("Redis connection refused");

        // Verify none of the error handler methods re-throw exceptions
        assertThatCode(() -> errorHandler.handleCacheGetError(redisException, mockCache, "key1"))
                .doesNotThrowAnyException();

        assertThatCode(() -> errorHandler.handleCachePutError(redisException, mockCache, "key1", "val1"))
                .doesNotThrowAnyException();

        assertThatCode(() -> errorHandler.handleCacheEvictError(redisException, mockCache, "key1"))
                .doesNotThrowAnyException();

        assertThatCode(() -> errorHandler.handleCacheClearError(redisException, mockCache))
                .doesNotThrowAnyException();
    }
}
