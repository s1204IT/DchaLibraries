package com.android.org.bouncycastle.util.encoders;

public class EncoderException extends IllegalStateException {
    private Throwable cause;

    EncoderException(String str, Throwable th) {
        super(str);
        this.cause = th;
    }

    @Override
    public Throwable getCause() {
        return this.cause;
    }
}
