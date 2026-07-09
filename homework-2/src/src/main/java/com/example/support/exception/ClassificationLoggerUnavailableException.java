package com.example.support.exception;

public class ClassificationLoggerUnavailableException extends RuntimeException {

    public ClassificationLoggerUnavailableException() {
        super("Classification logger unavailable; override rejected");
    }

    public ClassificationLoggerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
