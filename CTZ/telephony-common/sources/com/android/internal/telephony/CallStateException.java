package com.android.internal.telephony;

public class CallStateException extends Exception {
    public static final int ERROR_INVALID = -1;
    public static final int ERROR_OUT_OF_SERVICE = 1;
    public static final int ERROR_POWER_OFF = 2;
    private int mError;

    public CallStateException() {
        this.mError = -1;
    }

    public CallStateException(String str) {
        super(str);
        this.mError = -1;
    }

    public CallStateException(int i, String str) {
        super(str);
        this.mError = -1;
        this.mError = i;
    }

    public int getError() {
        return this.mError;
    }
}
