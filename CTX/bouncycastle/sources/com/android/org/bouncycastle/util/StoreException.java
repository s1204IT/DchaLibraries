package com.android.org.bouncycastle.util;

public class StoreException extends RuntimeException {
    private Throwable _e;

    public StoreException(String str, Throwable th) {
        super(str);
        this._e = th;
    }

    @Override
    public Throwable getCause() {
        return this._e;
    }
}
