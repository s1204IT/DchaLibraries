package com.android.mms.service.exception;

public class MmsHttpException extends Exception {
    private final int mStatusCode;

    public MmsHttpException(int statusCode, String message) {
        super(message);
        this.mStatusCode = statusCode;
    }

    public MmsHttpException(int statusCode, Throwable cause) {
        super(cause);
        this.mStatusCode = statusCode;
    }

    public MmsHttpException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.mStatusCode = statusCode;
    }

    public int getStatusCode() {
        return this.mStatusCode;
    }
}
