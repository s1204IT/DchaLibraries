package android.security;

public class KeyStoreException extends Exception {
    private final int mErrorCode;

    public KeyStoreException(int i, String str) {
        super(str);
        this.mErrorCode = i;
    }

    public int getErrorCode() {
        return this.mErrorCode;
    }
}
