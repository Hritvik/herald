package com.vik.herald.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@Data
@Slf4j
@ConfigurationProperties(prefix = "herald")
public class RestClientConfigProperties {

    private Map<String, RestServiceConfig> services;

    @Data
    public static class RestServiceConfig {
        @JsonProperty("activate-at-startup")
        private boolean activateAtStartup;

        @JsonProperty("connection-pool")
        private HttpPoolConfigs connectionPool;

        private Credentials credentials;
        private Timeout timeout;
        private RetryConfig retry;

        @JsonProperty("circuit-breaker")
        private CircuitBreakerConfig circuitBreaker;

        private Map<String, EndpointConfig> endpoints;

        public RetryConfig getMergedRetryConfig(String pathIdentifier) {
            RetryConfig endpointRetryConfig = endpoints.get(pathIdentifier).getRetry();
            
            if (endpointRetryConfig == null) {
                return retry;
            }
            
            RetryConfig mergedConfig = new RetryConfig();
            
            // Copy service level config as base
            if (retry != null) {
                mergedConfig.setMaxAttempts(retry.getMaxAttempts());
                mergedConfig.setInitialInterval(retry.getInitialInterval());
                mergedConfig.setMultiplier(retry.getMultiplier());
                mergedConfig.setMaxInterval(retry.getMaxInterval());
                mergedConfig.setRetryableStatusCodes(retry.getRetryableStatusCodes());
                mergedConfig.setRetryableExceptions(retry.getRetryableExceptions());
                mergedConfig.setStrategy(retry.getStrategy());
            }
            
            // Override with endpoint level config if present
            if (endpointRetryConfig.getMaxAttempts() > 0) {
                mergedConfig.setMaxAttempts(endpointRetryConfig.getMaxAttempts());
            }
            if (endpointRetryConfig.getInitialInterval() > 0) {
                mergedConfig.setInitialInterval(endpointRetryConfig.getInitialInterval());
            }
            if (endpointRetryConfig.getMultiplier() > 0) {
                mergedConfig.setMultiplier(endpointRetryConfig.getMultiplier());
            }
            if (endpointRetryConfig.getMaxInterval() > 0) {
                mergedConfig.setMaxInterval(endpointRetryConfig.getMaxInterval());
            }
            if (endpointRetryConfig.getRetryableStatusCodes() != null && !endpointRetryConfig.getRetryableStatusCodes().isEmpty()) {
                mergedConfig.setRetryableStatusCodes(endpointRetryConfig.getRetryableStatusCodes());
            }
            if (endpointRetryConfig.getRetryableExceptions() != null && !endpointRetryConfig.getRetryableExceptions().isEmpty()) {
                mergedConfig.setRetryableExceptions(endpointRetryConfig.getRetryableExceptions());
            }
            if (endpointRetryConfig.getStrategy() != null) {
                mergedConfig.setStrategy(endpointRetryConfig.getStrategy());
            }
            
            return mergedConfig;
        }

        public Timeout getMergedTimeoutConfig(String pathIdentifier) {
            Timeout endpointTimeout = endpoints.get(pathIdentifier).getTimeout();
            
            if (endpointTimeout == null) {
                return timeout;
            }
            
            Timeout mergedConfig = new Timeout();
            
            // Copy service level config as base
            if (timeout != null) {
                mergedConfig.setConnectionTimeout(timeout.getConnectionTimeout());
                mergedConfig.setRequestTimeout(timeout.getRequestTimeout());
                mergedConfig.setSocketTimeout(timeout.getSocketTimeout());
            }
            
            // Override with endpoint level config if present
            if (endpointTimeout.getConnectionTimeout() > 0) {
                mergedConfig.setConnectionTimeout(endpointTimeout.getConnectionTimeout());
            }
            if (endpointTimeout.getRequestTimeout() > 0) {
                mergedConfig.setRequestTimeout(endpointTimeout.getRequestTimeout());
            }
            if (endpointTimeout.getSocketTimeout() > 0) {
                mergedConfig.setSocketTimeout(endpointTimeout.getSocketTimeout());
            }
            
            return mergedConfig;
        }

        public CircuitBreakerConfig getMergedCircuitBreakerConfig(String pathIdentifier) {
            CircuitBreakerConfig endpointCBConfig = endpoints.get(pathIdentifier).getCircuitBreaker();
            
            if (endpointCBConfig == null) {
                return circuitBreaker;
            }
            
            CircuitBreakerConfig mergedConfig = new CircuitBreakerConfig();
            
            // Copy service level config as base
            if (circuitBreaker != null) {
                mergedConfig.setStrategy(circuitBreaker.getStrategy());
                mergedConfig.setSpringCB(circuitBreaker.getSpringCB());
            }
            
            // Override with endpoint level config if present
            if (endpointCBConfig.getStrategy() != null) {
                mergedConfig.setStrategy(endpointCBConfig.getStrategy());
            }
            if (endpointCBConfig.getSpringCB() != null) {
                mergedConfig.setSpringCB(endpointCBConfig.getSpringCB());
            }
            
            return mergedConfig;
        }
    }

    @Data
    public static class HttpPoolConfigs {
        private int maxIdleTime;
        private int maxTotal;
        private int defaultMaxPerRoute;
    }

    @Data
    public static class Credentials {
        private String host;
        private List<AuthHeader> auth;
    }

    @Data
    public static class AuthHeader {
        private String key;
        private String value;
    }

    @Data
    public static class EndpointConfig {
        private String path;

        @JsonProperty("circuit-breaker")
        private CircuitBreakerConfig circuitBreaker;

        @JsonProperty("retry")
        private RetryConfig retry;

        @JsonProperty("timeout")
        private Timeout timeout;
    }

    @Data
    public static class Timeout {
        private int connectionTimeout;
        private int requestTimeout;
        private int socketTimeout;
    }

    @Data
    public static class CircuitBreakerConfig {
        private String strategy;
        private SpringCBConfig springCB;
    }

    @Data
    public static class SpringCBConfig {
        @JsonProperty("circuit-breaker-name")
        private String circuitBreakerName;

        @JsonProperty("time-limiter-name")
        private String timeLimiterName;
    }

    @Data
    public static class RetryConfig {
        @JsonProperty("max-attempts")
        private int maxAttempts;

        @JsonProperty("initial-interval")
        private long initialInterval;

        @JsonProperty("multiplier")
        private double multiplier;

        @JsonProperty("max-interval")
        private long maxInterval;

        @JsonProperty("retryable-status-codes")
        private List<Integer> retryableStatusCodes;

        @JsonProperty("retryable-exceptions")
        private List<String> retryableExceptions;

        private String strategy;
    }

}
