package com.vik.herald.exceptions;

import lombok.Getter;

@Getter
public class DownStreamException extends RuntimeException {
    private Integer statusCode;
    private String message;

    public DownStreamException(Integer statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.message = message;
    }
}
