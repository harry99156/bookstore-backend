package com.pageturner.bookstore.exception;

/** Malformed request that bean validation cannot express; maps to HTTP 400. */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
