package com.vik.herald.clients.circuitbreaker;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Interface for circuit breaker strategy implementations.
 * This allows for different circuit breaker implementations to be used interchangeably.
 */
public interface CircuitBreakerStrategy {
    /**
     * Executes the given operation with circuit breaker protection.
     *
     * @param clientIdentifier The identifier of the client configuration
     * @param pathIdentifier The identifier of the endpoint path
     * @param operation The operation to execute
     * @param fallback The fallback function to execute if the operation fails
     * @param <R> The type of the result
     * @return A CompletableFuture that will complete with the result
     */
    <R> CompletableFuture<R> execute(
            String clientIdentifier,
            String pathIdentifier,
            Supplier<R> operation,
            Function<Throwable, R> fallback
    );
} 