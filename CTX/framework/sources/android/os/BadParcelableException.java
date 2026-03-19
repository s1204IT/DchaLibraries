package android.os;

import android.util.AndroidRuntimeException;

public class BadParcelableException extends AndroidRuntimeException {
    public BadParcelableException(String str) {
        super(str);
    }

    public BadParcelableException(Exception exc) {
        super(exc);
    }
}
