package com.android.server.tv;

import android.os.IBinder;
import dalvik.system.CloseGuard;
import java.io.IOException;

public final class UinputBridge {
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private long mPtr;
    private IBinder mToken;

    private static native void nativeClear(long j);

    private static native void nativeClose(long j);

    private static native long nativeOpen(String str, String str2, int i, int i2, int i3);

    private static native void nativeSendKey(long j, int i, boolean z);

    private static native void nativeSendPointerDown(long j, int i, int i2, int i3);

    private static native void nativeSendPointerSync(long j);

    private static native void nativeSendPointerUp(long j, int i);

    private static native void nativeSendTimestamp(long j, long j2);

    public UinputBridge(IBinder iBinder, String str, int i, int i2, int i3) throws IOException {
        this.mToken = null;
        if (i < 1 || i2 < 1) {
            throw new IllegalArgumentException("Touchpad must be at least 1x1.");
        }
        if (i3 < 1 || i3 > 32) {
            throw new IllegalArgumentException("Touchpad must support between 1 and 32 pointers.");
        }
        if (iBinder == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }
        this.mPtr = nativeOpen(str, iBinder.toString(), i, i2, i3);
        if (this.mPtr == 0) {
            throw new IOException("Could not open uinput device " + str);
        }
        this.mToken = iBinder;
        this.mCloseGuard.open("close");
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            close(this.mToken);
        } finally {
            this.mToken = null;
            super.finalize();
        }
    }

    public void close(IBinder iBinder) {
        if (isTokenValid(iBinder) && this.mPtr != 0) {
            clear(iBinder);
            nativeClose(this.mPtr);
            this.mPtr = 0L;
            this.mCloseGuard.close();
        }
    }

    public IBinder getToken() {
        return this.mToken;
    }

    protected boolean isTokenValid(IBinder iBinder) {
        return this.mToken.equals(iBinder);
    }

    public void sendTimestamp(IBinder iBinder, long j) {
        if (isTokenValid(iBinder)) {
            nativeSendTimestamp(this.mPtr, j);
        }
    }

    public void sendKeyDown(IBinder iBinder, int i) {
        if (isTokenValid(iBinder)) {
            nativeSendKey(this.mPtr, i, true);
        }
    }

    public void sendKeyUp(IBinder iBinder, int i) {
        if (isTokenValid(iBinder)) {
            nativeSendKey(this.mPtr, i, false);
        }
    }

    public void sendPointerDown(IBinder iBinder, int i, int i2, int i3) {
        if (isTokenValid(iBinder)) {
            nativeSendPointerDown(this.mPtr, i, i2, i3);
        }
    }

    public void sendPointerUp(IBinder iBinder, int i) {
        if (isTokenValid(iBinder)) {
            nativeSendPointerUp(this.mPtr, i);
        }
    }

    public void sendPointerSync(IBinder iBinder) {
        if (isTokenValid(iBinder)) {
            nativeSendPointerSync(this.mPtr);
        }
    }

    public void clear(IBinder iBinder) {
        if (isTokenValid(iBinder)) {
            nativeClear(this.mPtr);
        }
    }
}
