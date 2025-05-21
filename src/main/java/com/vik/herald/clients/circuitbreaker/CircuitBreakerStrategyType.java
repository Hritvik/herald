package com.vik.herald.clients.circuitbreaker;

import lombok.*;

/**
 * Enum representing the available circuit breaker strategy types.
 */
@Getter
public enum CircuitBreakerStrategyType {
    SPRING_CLOUD("SpringCloudCircuitBreakerStrategy");

    private final String className;

    CircuitBreakerStrategyType(String className) {
        this.className = className;
    }

    public static CircuitBreakerStrategyType fromClassName(String className) {
        for (CircuitBreakerStrategyType type : values()) {
            if (type.getClassName().equals(className)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown circuit breaker strategy: " + className);
    }
} 