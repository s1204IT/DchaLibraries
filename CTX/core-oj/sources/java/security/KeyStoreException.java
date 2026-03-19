package java.security;

public class KeyStoreException extends GeneralSecurityException {
    private static final long serialVersionUID = -1119353179322377262L;

    public KeyStoreException() {
    }

    public KeyStoreException(String str) {
        super(str);
    }

    public KeyStoreException(String str, Throwable th) {
        super(str, th);
    }

    public KeyStoreException(Throwable th) {
        super(th);
    }
}
