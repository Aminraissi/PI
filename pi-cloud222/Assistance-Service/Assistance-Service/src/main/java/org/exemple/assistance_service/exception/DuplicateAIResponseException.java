package org.exemple.assistance_service.exception;

public class DuplicateAIResponseException extends RuntimeException {
    public DuplicateAIResponseException(String message) {
        super(message);
    }
}
