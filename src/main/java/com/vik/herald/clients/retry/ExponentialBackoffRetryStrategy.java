package com.vik.herald.clients.retry;

import com.vik.herald.config.RestClientConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.function.Predicate;

@Slf4j
@Component
public class ExponentialBackoffRetryStrategy implements RetryStrategy {
    @Override
    public <T> T execute(
            Callable<T> operation,
            RestClientConfigProperties.RetryConfig retryConfig,
            Predicate<Throwable> shouldRetry
    ) throws Exception {
        int attempts = 0;
        long interval = retryConfig.getInitialInterval();
        
        while (true) {
            attempts++;
            try {
                return operation.call();
            } catch (Exception e) {
                if (attempts >= retryConfig.getMaxAttempts() || !shouldRetry.test(e)) {
                    throw e;
                }
                
                log.warn("Retry attempt {} of {} for operation", attempts, retryConfig.getMaxAttempts(), e);
                
                // Calculate next interval with exponential backoff
                interval = Math.min(
                        (long) (interval * retryConfig.getMultiplier()),
                        retryConfig.getMaxInterval()
                );
                
                Thread.sleep(interval);
            }
        }
    }
} 