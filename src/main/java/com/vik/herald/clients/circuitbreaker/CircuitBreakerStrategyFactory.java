package com.vik.herald.clients.circuitbreaker;

import com.vik.herald.config.RestClientConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.EnumMap;

@Component
@RequiredArgsConstructor
public class CircuitBreakerStrategyFactory {
    @Autowired private final List<CircuitBreakerStrategy> strategies;
    @Autowired private final RestClientConfigProperties restClientConfigProperties;

    private Map<CircuitBreakerStrategyType, CircuitBreakerStrategy> strategyMap;

    public CircuitBreakerStrategy getStrategy(String clientIdentifier, String pathIdentifier) {
        if (strategyMap == null) {
            strategyMap = new EnumMap<>(CircuitBreakerStrategyType.class);
            strategies.forEach(strategy -> {
                CircuitBreakerStrategyType type = CircuitBreakerStrategyType.fromClassName(
                    strategy.getClass().getSimpleName()
                );
                strategyMap.put(type, strategy);
            });
        }

        RestClientConfigProperties.RestServiceConfig restServiceConfig = restClientConfigProperties.getServices().get(clientIdentifier);
        RestClientConfigProperties.CircuitBreakerConfig circuitBreakerConfig = restServiceConfig.getMergedCircuitBreakerConfig(pathIdentifier);

        // If no strategy is configured, use Spring Cloud circuit breaker as default
        if (circuitBreakerConfig == null || circuitBreakerConfig.getStrategy() == null) {
            return strategyMap.get(CircuitBreakerStrategyType.SPRING_CLOUD);
        }

        // Get the configured strategy
        CircuitBreakerStrategyType strategyType = CircuitBreakerStrategyType.fromClassName(circuitBreakerConfig.getStrategy());
        CircuitBreakerStrategy strategy = strategyMap.get(strategyType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown circuit breaker strategy: " + circuitBreakerConfig.getStrategy());
        }

        return strategy;
    }
} 