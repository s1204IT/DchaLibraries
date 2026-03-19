package android.security.keystore;

import java.security.InvalidKeyException;

public class UserNotAuthenticatedException extends InvalidKeyException {
    public UserNotAuthenticatedException() {
        super("User not authenticated");
    }

    public UserNotAuthenticatedException(String str) {
        super(str);
    }

    public UserNotAuthenticatedException(String str, Throwable th) {
        super(str, th);
    }
}
