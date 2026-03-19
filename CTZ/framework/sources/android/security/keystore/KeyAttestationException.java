package android.security.keystore;

public class KeyAttestationException extends Exception {
    public KeyAttestationException(String str) {
        super(str);
    }

    public KeyAttestationException(String str, Throwable th) {
        super(str, th);
    }
}
