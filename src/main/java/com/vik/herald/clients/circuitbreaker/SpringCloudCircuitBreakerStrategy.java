package com.vik.herald.clients.circuitbreaker;

import com.vik.herald.config.RestClientConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.*;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringCloudCircuitBreakerStrategy implements CircuitBreakerStrategy {
    @Autowired private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    @Autowired private final RestClientConfigProperties restClientConfigProperties;
    @Autowired private final ExecutorService restClientExecutor;

    @Override
    public <R> CompletableFuture<R> execute(
            String clientIdentifier,
            String pathIdentifier,
            Supplier<R> operation,
            Function<Throwable, R> fallback
    ) {
        RestClientConfigProperties.RestServiceConfig restServiceConfig = restClientConfigProperties.getServices().get(clientIdentifier);
        RestClientConfigProperties.SpringCBConfig springCBConfig = restServiceConfig.getEndpoints()
                .get(pathIdentifier)
                .getCircuitBreaker()
                .getSpringCB();

        CircuitBreaker circuitBreaker = circuitBreakerFactory.create(springCBConfig.getCircuitBreakerName());

        return CompletableFuture.supplyAsync(
                () -> circuitBreaker.run(operation, fallback),
                restClientExecutor
        );
    }
} 