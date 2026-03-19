package android.content;

import android.util.AndroidRuntimeException;

public class ReceiverCallNotAllowedException extends AndroidRuntimeException {
    public ReceiverCallNotAllowedException(String str) {
        super(str);
    }
}
