package com.android.org.bouncycastle.jce.provider;

import java.security.cert.CRLException;

class ExtCRLException extends CRLException {
    Throwable cause;

    ExtCRLException(String str, Throwable th) {
        super(str);
        this.cause = th;
    }

    @Override
    public Throwable getCause() {
        return this.cause;
    }
}
