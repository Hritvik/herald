package com.vik.herald.clients;

import com.vik.herald.aop.annotations.*;
import com.vik.herald.config.*;
import org.apache.commons.lang3.tuple.*;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.*;
import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.client5.http.protocol.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.*;
import org.apache.hc.core5.net.*;
import org.springframework.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.*;

import java.net.*;
import java.nio.charset.*;
import java.util.*;

@Service
public class RestClientHelper {


    @PooledDownstreamLog
    public Pair<Integer, String> post(
            CloseableHttpClient httpClient,
            String url,
            Map<String, String> headers,
            String requestString,
            String contentType,
            RequestConfig requestConfig)
            throws Exception {

        URI uri = new URI(url);
        HttpHost target = new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort());

        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(
                requestString,
                ContentType.create(contentType, StandardCharsets.UTF_8)
        ));

        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                httpPost.setHeader(header.getKey(), header.getValue());
            }
        }

        HttpClientContext context = HttpClientContext.create();
        context.setRequestConfig(requestConfig);

        return httpClient.execute(target, httpPost, context, response -> {
            int statusCode = response.getCode();
            String responseString = "";
            if (response.getEntity() != null) {
                responseString = EntityUtils.toString(response.getEntity());
            }
            return Pair.of(statusCode, responseString);
        });
    }


    @PooledDownstreamLog
    public Pair<Integer, String> get(
            CloseableHttpClient httpClient,
            String url,
            Map<String, String> headers,
            Map<String, String> requestParameters,
            RequestConfig requestConfig)
            throws Exception {

        URIBuilder uriBuilder = new URIBuilder(url);
        if (requestParameters != null && !requestParameters.isEmpty()) {
            for (Map.Entry<String, String> entry : requestParameters.entrySet()) {
                uriBuilder.addParameter(entry.getKey(), entry.getValue());
            }
        }
        URI requestUri = uriBuilder.build();

        HttpGet httpGet = new HttpGet(requestUri);

        // Set headers
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                httpGet.setHeader(header.getKey(), header.getValue());
            }
        }

        // Setup context with request config (instead of deprecated httpGet.setConfig)
        HttpClientContext context = HttpClientContext.create();
        context.setRequestConfig(requestConfig);

        // Execute request with ResponseHandler for auto resource management
        return httpClient.execute(httpGet, context, response -> {
            int statusCode = response.getCode();
            String responseString = "";
            if (response.getEntity() != null) {
                responseString = EntityUtils.toString(response.getEntity());
            }
            return Pair.of(statusCode, responseString);
        });
    }


    public Map<String, String> prepareHeaders(Map<String, String> headers, RestClientConfigProperties.Credentials serviceDetails) {
        if (headers == null) {
            headers = new HashMap<>();
        }

        // Add default headers
        headers.putIfAbsent(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        // Add auth headers
        if (serviceDetails.getAuth() != null) {
            for (RestClientConfigProperties.AuthHeader auth : serviceDetails.getAuth()) {
                headers.putIfAbsent(auth.getKey(), auth.getValue());
            }
        }

        return headers;
    }


    public boolean isRetryableStatusCode(int statusCode, List<Integer> retryableStatusCodes) {
        return retryableStatusCodes != null && retryableStatusCodes.contains(statusCode);
    }

    public boolean isRetryableException(Throwable e, List<String> retryableExceptions) {
        if (e == null) {
            return false;
        }

        String exceptionName = e.getClass().getName();
        if (retryableExceptions.contains(exceptionName)) {
            return true;
        }

        // Check if any of the cause exceptions are retryable
        Throwable cause = e.getCause();
        while (cause != null) {
            if (retryableExceptions.contains(cause.getClass().getName())) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }


}
