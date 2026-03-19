package android.view;

import android.util.AndroidRuntimeException;

final class WindowLeaked extends AndroidRuntimeException {
    public WindowLeaked(String str) {
        super(str);
    }
}
