package com.pageturner.bookstore.exception;

/** The AI backend (e.g. Ollama) is unreachable or failed; maps to HTTP 503. */
public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
