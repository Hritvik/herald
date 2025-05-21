package com.vik.herald.utils;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.function.Supplier;

@Component
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementCounter(String metricName, String... tags) {
        Tags micrometerTags = Tags.of(tags);

        Counter counter = Counter.builder(metricName)
                .tags(micrometerTags)
                .register(meterRegistry);

        counter.increment();
    }

    public void recordDuration(String metricName, Runnable runnable, String... tags) {
        Tags micrometerTags = Tags.of(tags);
        Timer timer = Timer.builder(metricName)
                .tags(micrometerTags)
                .register(meterRegistry);

        timer.record(runnable);
    }

    public <T> T recordDuration(String metricName, Supplier<T> supplier, String... tags) {
        Tags micrometerTags = Tags.of(tags);
        Timer timer = Timer.builder(metricName)
                .tags(micrometerTags)
                .register(meterRegistry);

        return timer.record(supplier);
    }

    public void registerGauge(String metricName, AtomicInteger value, String... tags) {
        Tags micrometerTags = Tags.of(tags);

        Gauge.builder(metricName, value, AtomicInteger::get)
                .tags(micrometerTags)
                .register(meterRegistry);
    }
}
