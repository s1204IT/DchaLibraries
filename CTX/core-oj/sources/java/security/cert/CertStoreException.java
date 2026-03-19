package java.security.cert;

import java.security.GeneralSecurityException;

public class CertStoreException extends GeneralSecurityException {
    private static final long serialVersionUID = 2395296107471573245L;

    public CertStoreException() {
    }

    public CertStoreException(String str) {
        super(str);
    }

    public CertStoreException(Throwable th) {
        super(th);
    }

    public CertStoreException(String str, Throwable th) {
        super(str, th);
    }
}
