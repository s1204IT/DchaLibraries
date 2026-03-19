package java.security.cert;

public class CertificateParsingException extends CertificateException {
    private static final long serialVersionUID = -7989222416793322029L;

    public CertificateParsingException() {
    }

    public CertificateParsingException(String str) {
        super(str);
    }

    public CertificateParsingException(String str, Throwable th) {
        super(str, th);
    }

    public CertificateParsingException(Throwable th) {
        super(th);
    }
}
