package com.vik.herald.clients;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vik.herald.clients.circuitbreaker.CircuitBreakerStrategyFactory;
import com.vik.herald.clients.retry.RetryStrategyFactory;
import com.vik.herald.config.RestClientConfigProperties;
import com.vik.herald.exceptions.RetryableHttpException;
import com.vik.herald.interfaces.*;
import com.vik.herald.utils.LoggingMethods;
import com.vik.herald.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RestClient implements Herald {
    protected final ObjectMapper objectMapper;
    protected final LoggingMethods loggingMethods;
    protected final RestClientConfigProperties restClientConfigProperties;
    protected final HttpClientFactory httpClientFactory;
    protected final CircuitBreakerStrategyFactory circuitBreakerStrategyFactory;
    protected final RetryStrategyFactory retryStrategyFactory;
    protected final RestClientHelper restClientHelper;

    @Override
    public <T, R> CompletableFuture<R> post(
            String clientIdentifier,
            String pathIdentifier,
            T request,
            Map<String, String> headers,
            TypeReference<R> responseType,
            Supplier<R> fallback
    ) {
        CloseableHttpClient httpClient = httpClientFactory.getClient(clientIdentifier);
        RestClientConfigProperties.RestServiceConfig restServiceConfig = restClientConfigProperties.getServices().get(clientIdentifier);
        RestClientConfigProperties.Credentials credentials = restServiceConfig.getCredentials();
        RestClientConfigProperties.EndpointConfig endpointConfig = restServiceConfig.getEndpoints().get(pathIdentifier);
        String host = credentials.getHost();
        String path = endpointConfig.getPath();
        RestClientConfigProperties.RetryConfig retryConfig = restServiceConfig.getMergedRetryConfig(pathIdentifier);
        RestClientConfigProperties.Timeout timeout = restServiceConfig.getMergedTimeoutConfig(pathIdentifier);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(timeout.getRequestTimeout()))
                .setResponseTimeout(Timeout.ofMilliseconds(timeout.getSocketTimeout()))
                .build();

        Map<String, String> finalHeaders = restClientHelper.prepareHeaders(headers, credentials);

        return circuitBreakerStrategyFactory
                .getStrategy(clientIdentifier, pathIdentifier)
                .execute(
                        clientIdentifier,
                        pathIdentifier,
                        () -> {
                            try {
                                String uri = host + path;
                                String requestString = objectMapper.writeValueAsString(request);

                                return retryStrategyFactory.getStrategy(clientIdentifier, pathIdentifier).execute(
                                        () -> {
                                            Pair<Integer, String> response = restClientHelper.post(
                                                    httpClient, uri, finalHeaders, requestString, MediaType.APPLICATION_JSON_VALUE, requestConfig);

                                            if (restClientHelper.isRetryableStatusCode(response.getLeft(), retryConfig.getRetryableStatusCodes())) {
                                                throw new RetryableHttpException(response.getLeft(), response.getRight());
                                            }

                                            R result = ResponseUtils.commandResponseHandlerV3(response, responseType);
                                            loggingMethods.logDownstreamRequestResponse(clientIdentifier, host, path, result, request);
                                            return result;
                                        },
                                        retryConfig,
                                        e -> restClientHelper.isRetryableException(e, retryConfig.getRetryableExceptions())
                                );
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        },
                        (throwable) -> {
                            loggingMethods.logDownstreamFallback(clientIdentifier, host, path, throwable, request);
                            return fallback.get();
                        }
                );
    }

    @Override
    public <R> CompletableFuture<R> get(
            String clientIdentifier,
            String pathIdentifier,
            Map<String, String> queryParams,
            Map<String, String> headers,
            TypeReference<R> responseType,
            Supplier<R> fallback
    ) {
        CloseableHttpClient httpClient = httpClientFactory.getClient(clientIdentifier);
        RestClientConfigProperties.RestServiceConfig restServiceConfig = restClientConfigProperties.getServices().get(clientIdentifier);
        RestClientConfigProperties.Credentials credentials = restServiceConfig.getCredentials();
        String host = credentials.getHost();
        String path = restServiceConfig.getEndpoints().get(pathIdentifier).getPath();
        RestClientConfigProperties.RetryConfig retryConfig = restServiceConfig.getMergedRetryConfig(pathIdentifier);
        RestClientConfigProperties.Timeout timeout = restServiceConfig.getMergedTimeoutConfig(pathIdentifier);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(timeout.getRequestTimeout()))
                .setResponseTimeout(Timeout.ofMilliseconds(timeout.getSocketTimeout()))
                .build();

        Map<String, String> finalHeaders = restClientHelper.prepareHeaders(headers, credentials);

        return circuitBreakerStrategyFactory
                .getStrategy(clientIdentifier, pathIdentifier)
                .execute(
                        clientIdentifier,
                        pathIdentifier,
                        () -> {
                            try {
                                String uri = host + path;

                                return retryStrategyFactory.getStrategy(clientIdentifier, pathIdentifier).execute(
                                        () -> {
                                            Pair<Integer, String> response = restClientHelper.get(
                                                    httpClient, uri, finalHeaders, queryParams, requestConfig);

                                            if (restClientHelper.isRetryableStatusCode(response.getLeft(), retryConfig.getRetryableStatusCodes())) {
                                                throw new RetryableHttpException(response.getLeft(), response.getRight());
                                            }

                                            R result = ResponseUtils.commandResponseHandlerV3(response, responseType);
                                            loggingMethods.logDownstreamRequestResponse(clientIdentifier, host, path, result, queryParams);
                                            return result;
                                        },
                                        retryConfig,
                                        e -> restClientHelper.isRetryableException(e, retryConfig.getRetryableExceptions())
                                );
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        },
                        (throwable) -> {
                            loggingMethods.logDownstreamFallback(clientIdentifier, host, path, throwable, queryParams);
                            return fallback.get();
                        }
                );
    }

    @Override
    public <T, R> CompletableFuture<R> post(
            String clientIdentifier,
            String pathIdentifier,
            T request,
            Map<String, String> headers,
            TypeReference<R> responseType
    ) {
        return post(clientIdentifier, pathIdentifier, request, headers, responseType, () -> null);
    }

    @Override
    public <R> CompletableFuture<R> get(
            String clientIdentifier,
            String pathIdentifier,
            Map<String, String> queryParams,
            Map<String, String> headers,
            TypeReference<R> responseType
    ) {
        return get(clientIdentifier, pathIdentifier, queryParams, headers, responseType, () -> null);
    }
}