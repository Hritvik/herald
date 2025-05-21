package com.vik.herald.interfaces;

import com.fasterxml.jackson.core.type.TypeReference;
import com.vik.herald.clients.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Herald is a high-level HTTP client interface that provides a simple way to make HTTP requests
 * with built-in support for circuit breaking, retries, and metrics.
 */
public interface Herald extends IRestClient {
    /**
     * Makes a POST request to the specified endpoint.
     *
     * @param clientIdentifier The identifier of the client configuration to use
     * @param pathIdentifier The identifier of the endpoint path to use
     * @param request The request body
     * @param headers Optional HTTP headers
     * @param responseType The type of the response
     * @param fallback A supplier that provides a fallback response in case of failure
     * @param <T> The type of the request
     * @param <R> The type of the response
     * @return A CompletableFuture that will complete with the response
     */
    @Override
    <T, R> CompletableFuture<R> post(
            String clientIdentifier,
            String pathIdentifier,
            T request,
            Map<String, String> headers,
            TypeReference<R> responseType,
            Supplier<R> fallback
    );

    /**
     * Makes a POST request to the specified endpoint without a fallback.
     *
     * @param clientIdentifier The identifier of the client configuration to use
     * @param pathIdentifier The identifier of the endpoint path to use
     * @param request The request body
     * @param headers Optional HTTP headers
     * @param responseType The type of the response
     * @param <T> The type of the request
     * @param <R> The type of the response
     * @return A CompletableFuture that will complete with the response
     */
    @Override
    <T, R> CompletableFuture<R> post(
            String clientIdentifier,
            String pathIdentifier,
            T request,
            Map<String, String> headers,
            TypeReference<R> responseType
    );

    /**
     * Makes a GET request to the specified endpoint.
     *
     * @param clientIdentifier The identifier of the client configuration to use
     * @param pathIdentifier The identifier of the endpoint path to use
     * @param queryParams Optional query parameters
     * @param headers Optional HTTP headers
     * @param responseType The type of the response
     * @param fallback A supplier that provides a fallback response in case of failure
     * @param <R> The type of the response
     * @return A CompletableFuture that will complete with the response
     */
    @Override
    <R> CompletableFuture<R> get(
            String clientIdentifier,
            String pathIdentifier,
            Map<String, String> queryParams,
            Map<String, String> headers,
            TypeReference<R> responseType,
            Supplier<R> fallback
    );

    /**
     * Makes a GET request to the specified endpoint without a fallback.
     *
     * @param clientIdentifier The identifier of the client configuration to use
     * @param pathIdentifier The identifier of the endpoint path to use
     * @param queryParams Optional query parameters
     * @param headers Optional HTTP headers
     * @param responseType The type of the response
     * @param <R> The type of the response
     * @return A CompletableFuture that will complete with the response
     */
    @Override
    <R> CompletableFuture<R> get(
            String clientIdentifier,
            String pathIdentifier,
            Map<String, String> queryParams,
            Map<String, String> headers,
            TypeReference<R> responseType
    );
} 