package com.droneops.exception;

/** İş kuralı ihlallerinde fırlatılır (HTTP 409). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
