package com.vik.herald.clients.retry;

import com.vik.herald.config.RestClientConfigProperties;

import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * Interface for retry strategy implementations.
 * This allows for different retry implementations to be used interchangeably.
 */
public interface RetryStrategy {
    /**
     * Executes the given operation with retry protection.
     *
     * @param operation The operation to execute
     * @param retryConfig The retry configuration
     * @param shouldRetry Predicate to determine if an exception should trigger a retry
     * @param <T> The type of the result
     * @return The result of the operation
     * @throws Exception if the operation fails after all retries
     */
    <T> T execute(
            Callable<T> operation,
            RestClientConfigProperties.RetryConfig retryConfig,
            Predicate<Throwable> shouldRetry
    ) throws Exception;
} 