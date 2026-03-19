package java.security;

public class InvalidAlgorithmParameterException extends GeneralSecurityException {
    private static final long serialVersionUID = 2864672297499471472L;

    public InvalidAlgorithmParameterException() {
    }

    public InvalidAlgorithmParameterException(String str) {
        super(str);
    }

    public InvalidAlgorithmParameterException(String str, Throwable th) {
        super(str, th);
    }

    public InvalidAlgorithmParameterException(Throwable th) {
        super(th);
    }
}
