package java.security.cert;

import java.security.GeneralSecurityException;

public class CertPathBuilderException extends GeneralSecurityException {
    private static final long serialVersionUID = 5316471420178794402L;

    public CertPathBuilderException() {
    }

    public CertPathBuilderException(String str) {
        super(str);
    }

    public CertPathBuilderException(Throwable th) {
        super(th);
    }

    public CertPathBuilderException(String str, Throwable th) {
        super(str, th);
    }
}
