package com.example.support.exception;

public class UnsupportedImportFormatException extends RuntimeException {

    private final String format;

    public UnsupportedImportFormatException(String type) {
        super("Unsupported import format: " + type);
        this.format = type;
    }

    public String getFormat() {
        return format;
    }
}
