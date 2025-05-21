package com.vik.herald.clients.retry;

public enum RetryStrategyType {
    EXPONENTIAL_BACKOFF("ExponentialBackoffRetryStrategy"),
    NO_RETRY("NoRetryStrategy");

    private final String className;

    RetryStrategyType(String className) {
        this.className = className;
    }

    public static RetryStrategyType fromClassName(String className) {
        for (RetryStrategyType type : values()) {
            if (type.className.equals(className)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown retry strategy: " + className);
    }

    public String getClassName() {
        return className;
    }
} 