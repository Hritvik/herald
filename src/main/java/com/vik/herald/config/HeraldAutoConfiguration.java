package com.vik.herald.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vik.herald.clients.*;
import com.vik.herald.clients.circuitbreaker.CircuitBreakerStrategyFactory;
import com.vik.herald.clients.circuitbreaker.SpringCloudCircuitBreakerStrategy;
import com.vik.herald.clients.filter.DownstreamRequestInterceptor;
import com.vik.herald.clients.RestClient;
import com.vik.herald.clients.retry.*;
import com.vik.herald.utils.LoggingMethods;
import com.vik.herald.utils.MetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.env.Environment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

@Configuration
@EnableConfigurationProperties(RestClientConfigProperties.class)
public class HeraldAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public MetricsService metricsService(MeterRegistry meterRegistry) {
        return new MetricsService(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public LoggingMethods loggingMethods(
            ObjectMapper objectMapper,
            Environment environment,
            MetricsService metricsService
    ) {
        return new LoggingMethods(objectMapper, environment, metricsService);
    }

    @Bean(name = "restClientExecutor")
    @ConditionalOnMissingBean(name = "restClientExecutor")
    public ExecutorService restClientExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpClientFactory httpClientRegistry(
            RestClientConfigProperties restClientConfigProperties
    ) {
        return new HttpClientFactory(
                new DownstreamRequestInterceptor(),
                restClientConfigProperties
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerStrategyFactory circuitBreakerStrategyFactory(
            RestClientConfigProperties restClientConfigProperties,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory,
            ExecutorService restClientExecutor
    ) {
        return new CircuitBreakerStrategyFactory(
                List.of(new SpringCloudCircuitBreakerStrategy(circuitBreakerFactory, restClientConfigProperties, restClientExecutor)),
                restClientConfigProperties
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public RetryStrategyFactory retryStrategyFactory(
            RestClientConfigProperties restClientConfigProperties
    ) {
        return new RetryStrategyFactory(
                List.of(
                        new ExponentialBackoffRetryStrategy(),
                        new NoRetryStrategy()
                ),
                restClientConfigProperties
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public RestClientHelper restClientHelper() {
        return new RestClientHelper();
    }

    @Bean
    @ConditionalOnMissingBean
    public RestClient restClient(
            ObjectMapper objectMapper,
            LoggingMethods loggingMethods,
            RestClientConfigProperties restClientConfigProperties,
            HttpClientFactory httpClientFactory,
            CircuitBreakerStrategyFactory circuitBreakerStrategyFactory,
            RetryStrategyFactory retryStrategyFactory,
            RestClientHelper restClientHelper
    ) {
        return new RestClient(
                objectMapper,
                loggingMethods,
                restClientConfigProperties,
                httpClientFactory,
                circuitBreakerStrategyFactory,
                retryStrategyFactory,
                restClientHelper
        );
    }
} 