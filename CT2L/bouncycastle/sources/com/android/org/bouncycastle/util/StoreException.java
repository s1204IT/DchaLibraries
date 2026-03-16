package com.android.org.bouncycastle.util;

public class StoreException extends RuntimeException {
    private Throwable _e;

    public StoreException(String s, Throwable e) {
        super(s);
        this._e = e;
    }

    @Override
    public Throwable getCause() {
        return this._e;
    }
}
