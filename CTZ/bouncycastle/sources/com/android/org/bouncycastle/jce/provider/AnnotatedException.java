package com.android.org.bouncycastle.jce.provider;

import com.android.org.bouncycastle.jce.exception.ExtException;

public class AnnotatedException extends Exception implements ExtException {
    private Throwable _underlyingException;

    public AnnotatedException(String str, Throwable th) {
        super(str);
        this._underlyingException = th;
    }

    public AnnotatedException(String str) {
        this(str, null);
    }

    Throwable getUnderlyingException() {
        return this._underlyingException;
    }

    @Override
    public Throwable getCause() {
        return this._underlyingException;
    }
}
