package com.vik.herald.clients.retry;

import com.vik.herald.config.RestClientConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.EnumMap;

@Component
@RequiredArgsConstructor
public class RetryStrategyFactory {
    private final List<RetryStrategy> strategies;
    private final RestClientConfigProperties restClientConfigProperties;

    private Map<RetryStrategyType, RetryStrategy> strategyMap;

    public RetryStrategy getStrategy(String clientIdentifier, String pathIdentifier) {
        if (strategyMap == null) {
            strategyMap = new EnumMap<>(RetryStrategyType.class);
            strategies.forEach(strategy -> {
                RetryStrategyType type = RetryStrategyType.fromClassName(
                    strategy.getClass().getSimpleName()
                );
                strategyMap.put(type, strategy);
            });
        }

        RestClientConfigProperties.RestServiceConfig restServiceConfig = restClientConfigProperties.getServices().get(clientIdentifier);
        RestClientConfigProperties.RetryConfig retryConfig = restServiceConfig.getMergedRetryConfig(pathIdentifier);

        // If retry is not configured or max attempts is 0, use no retry strategy
        if (retryConfig == null || retryConfig.getMaxAttempts() <= 0) {
            return strategyMap.get(RetryStrategyType.NO_RETRY);
        }

        // If no strategy is configured, use exponential backoff as default
        if (retryConfig.getStrategy() == null) {
            return strategyMap.get(RetryStrategyType.EXPONENTIAL_BACKOFF);
        }

        // Get the configured strategy
        RetryStrategyType strategyType = RetryStrategyType.fromClassName(retryConfig.getStrategy());
        RetryStrategy strategy = strategyMap.get(strategyType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown retry strategy: " + retryConfig.getStrategy());
        }

        return strategy;
    }
} 