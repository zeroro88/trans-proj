package com.transproj.client;

public class LlmException extends Exception {

    private final String errorCode;

    public LlmException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public LlmException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
