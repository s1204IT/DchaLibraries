package com.android.providers.downloads;

class StopRequestException extends Exception {
    private final int mFinalStatus;

    public StopRequestException(int finalStatus, String message) {
        super(message);
        this.mFinalStatus = finalStatus;
    }

    public StopRequestException(int finalStatus, Throwable t) {
        this(finalStatus, t.getMessage());
        initCause(t);
    }

    public StopRequestException(int finalStatus, String message, Throwable t) {
        this(finalStatus, message);
        initCause(t);
    }

    public int getFinalStatus() {
        return this.mFinalStatus;
    }

    public static StopRequestException throwUnhandledHttpError(int code, String message) throws StopRequestException {
        String error = "Unhandled HTTP response: " + code + " " + message;
        if (code >= 400 && code < 600) {
            throw new StopRequestException(code, error);
        }
        if (code >= 300 && code < 400) {
            throw new StopRequestException(493, error);
        }
        throw new StopRequestException(494, error);
    }
}
