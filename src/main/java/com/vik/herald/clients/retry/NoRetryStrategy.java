package com.vik.herald.clients.retry;

import com.vik.herald.config.RestClientConfigProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * A retry strategy that does not perform any retries.
 * This is useful when you want to disable retries for certain operations.
 */
@Component
public class NoRetryStrategy implements RetryStrategy {
    @Override
    public <T> T execute(
            Callable<T> operation,
            RestClientConfigProperties.RetryConfig retryConfig,
            Predicate<Throwable> shouldRetry
    ) throws Exception {
        return operation.call();
    }
} 