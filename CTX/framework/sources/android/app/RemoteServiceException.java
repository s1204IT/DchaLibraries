package android.app;

import android.util.AndroidRuntimeException;

final class RemoteServiceException extends AndroidRuntimeException {
    public RemoteServiceException(String str) {
        super(str);
    }
}
