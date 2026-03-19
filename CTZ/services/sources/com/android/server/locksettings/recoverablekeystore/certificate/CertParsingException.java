package com.android.server.locksettings.recoverablekeystore.certificate;

public class CertParsingException extends Exception {
    public CertParsingException(String str) {
        super(str);
    }

    public CertParsingException(Exception exc) {
        super(exc);
    }

    public CertParsingException(String str, Exception exc) {
        super(str, exc);
    }
}
