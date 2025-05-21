package com.vik.herald.exceptions;

public class InternalServerErrorException extends DownStreamException {
    private final Integer statusCode;
    private final String message;

    public InternalServerErrorException(Integer statusCode, String message) {
        super(statusCode, message);
        this.statusCode = statusCode;
        this.message = message;
    }
}
