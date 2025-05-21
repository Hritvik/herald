package com.vik.herald.clients;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface IRestClient {

    <T, R> CompletableFuture<R> post(
            String clientIdentifier,
            String pathIdentifier,
            T request,
            Map<String, String> headers,
            TypeReference<R> responseType,
            Supplier<R> fallback
    );

    <T, R> CompletableFuture<R> post(
            String clientIdentifier,
            String pathIdentifier,
            T request,
            Map<String, String> headers,
            TypeReference<R> responseType
    );

    <R> CompletableFuture<R> get(
            String clientIdentifier,
            String pathIdentifier,
            Map<String, String> queryParams,
            Map<String, String> headers,
            TypeReference<R> responseType,
            Supplier<R> fallback
    );

    <R> CompletableFuture<R> get(
            String clientIdentifier,
            String pathIdentifier,
            Map<String, String> queryParams,
            Map<String, String> headers,
            TypeReference<R> responseType
    );
}
