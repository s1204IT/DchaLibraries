package com.android.se.security.gpac;

public class DO_Exception extends Exception {
    private static final long serialVersionUID = -3917637590082486538L;

    public DO_Exception() {
    }

    public DO_Exception(String str, Throwable th) {
        super(str, th);
    }

    public DO_Exception(String str) {
        super(str);
    }

    public DO_Exception(Throwable th) {
        super(th);
    }
}
