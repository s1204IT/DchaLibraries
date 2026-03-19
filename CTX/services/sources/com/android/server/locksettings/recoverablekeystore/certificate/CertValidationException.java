package com.android.server.locksettings.recoverablekeystore.certificate;

public class CertValidationException extends Exception {
    public CertValidationException(String str) {
        super(str);
    }

    public CertValidationException(Exception exc) {
        super(exc);
    }

    public CertValidationException(String str, Exception exc) {
        super(str, exc);
    }
}
