package com.transproj.client;

public class MinerUException extends Exception {

    private final String errorCode;

    public MinerUException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public MinerUException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
