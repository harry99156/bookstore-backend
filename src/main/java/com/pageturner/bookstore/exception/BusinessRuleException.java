package com.pageturner.bookstore.exception;

/** Business-rule violation (duplicate ISBN, insufficient stock, empty cart, invalid state transition). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
