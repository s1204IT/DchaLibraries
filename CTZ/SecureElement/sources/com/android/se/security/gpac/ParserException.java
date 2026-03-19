package com.android.se.security.gpac;

public class ParserException extends Exception {
    private static final long serialVersionUID = -3917637590082486538L;

    public ParserException() {
    }

    public ParserException(String str, Throwable th) {
        super(str, th);
    }

    public ParserException(String str) {
        super(str);
    }

    public ParserException(Throwable th) {
        super(th);
    }
}
