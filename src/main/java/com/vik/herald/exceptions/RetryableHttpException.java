package com.vik.herald.exceptions;

public class RetryableHttpException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;

    public RetryableHttpException(int statusCode, String responseBody) {
        super("HTTP request failed with status code: " + statusCode);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
} 