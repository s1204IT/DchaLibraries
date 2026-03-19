package android.media;

import android.os.Handler;
import android.util.Slog;
import android.view.Surface;
import dalvik.system.CloseGuard;

public final class RemoteDisplay {
    public static final int DISPLAY_ERROR_CONNECTION_DROPPED = 2;
    public static final int DISPLAY_ERROR_UNKOWN = 1;
    public static final int DISPLAY_FLAG_SECURE = 1;
    private static final String TAG = "RemoteDisplay";
    private static boolean isDispose = false;
    private static final Object lock = new Object();
    private final CloseGuard mGuard = CloseGuard.get();
    private final Handler mHandler;
    private final Listener mListener;
    private final String mOpPackageName;
    private long mPtr;

    public interface Listener {
        void onDisplayConnected(Surface surface, int i, int i2, int i3, int i4);

        void onDisplayDisconnected();

        void onDisplayError(int i);

        void onDisplayGenericMsgEvent(int i);

        void onDisplayKeyEvent(int i, int i2);
    }

    private native long nativeConnect(String str, Surface surface);

    private native void nativeDispose(long j);

    private native int nativeGetWfdParam(long j, int i);

    private native long nativeListen(String str, String str2);

    private native void nativePause(long j);

    private native void nativeResume(long j);

    private native void nativeSendUibcEvent(long j, String str);

    private native void nativeSetBitrateControl(long j, int i);

    private native void nativeSuspendDisplay(long j, boolean z, Surface surface);

    private RemoteDisplay(Listener listener, Handler handler, String str) {
        this.mListener = listener;
        this.mHandler = handler;
        this.mOpPackageName = str;
    }

    protected void finalize() throws Throwable {
        try {
            dispose(true);
        } finally {
            super.finalize();
        }
    }

    public static RemoteDisplay listen(String str, Listener listener, Handler handler, String str2) {
        Slog.d(TAG, "listen");
        if (str == null) {
            throw new IllegalArgumentException("iface must not be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        RemoteDisplay remoteDisplay = new RemoteDisplay(listener, handler, str2);
        remoteDisplay.startListening(str);
        return remoteDisplay;
    }

    public void dispose() {
        Slog.d(TAG, "dispose");
        synchronized (lock) {
            if (isDispose) {
                Slog.d(TAG, "dispose done");
            } else {
                isDispose = true;
                dispose(false);
            }
        }
    }

    public void pause() {
        Slog.d(TAG, "pause");
        nativePause(this.mPtr);
    }

    public void resume() {
        Slog.d(TAG, "resume");
        nativeResume(this.mPtr);
    }

    private void dispose(boolean z) {
        Slog.d(TAG, "dispose");
        if (this.mPtr != 0) {
            if (this.mGuard != null) {
                if (z) {
                    this.mGuard.warnIfOpen();
                } else {
                    this.mGuard.close();
                }
            }
            nativeDispose(this.mPtr);
            this.mPtr = 0L;
        }
        synchronized (lock) {
            Slog.d(TAG, "dispose finish");
            isDispose = false;
        }
    }

    private void startListening(String str) {
        Slog.d(TAG, "startListening");
        this.mPtr = nativeListen(str, this.mOpPackageName);
        if (this.mPtr == 0) {
            throw new IllegalStateException("Could not start listening for remote display connection on \"" + str + "\"");
        }
        this.mGuard.open("dispose");
    }

    public void setBitrateControl(int i) {
        nativeSetBitrateControl(this.mPtr, i);
    }

    public int getWfdParam(int i) {
        return nativeGetWfdParam(this.mPtr, i);
    }

    public static RemoteDisplay connect(String str, Surface surface, Listener listener, Handler handler) {
        Slog.d(TAG, "connect");
        if (str == null) {
            throw new IllegalArgumentException("iface must not be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        RemoteDisplay remoteDisplay = new RemoteDisplay(listener, handler, null);
        remoteDisplay.startConnecting(str, surface);
        return remoteDisplay;
    }

    private void startConnecting(String str, Surface surface) {
        Slog.d(TAG, "startConnecting");
        this.mPtr = nativeConnect(str, surface);
        if (this.mPtr == 0) {
            throw new IllegalStateException("Could not start connecting for remote display connection on \"" + str + "\"");
        }
        this.mGuard.open("dispose");
    }

    public void suspendDisplay(boolean z, Surface surface) {
        Slog.d(TAG, "suspendDisplay");
        if (z && surface != null) {
            throw new IllegalArgumentException("surface must be null when suspend display");
        }
        if (!z && surface == null) {
            throw new IllegalArgumentException("surface must not be null when resume display");
        }
        nativeSuspendDisplay(this.mPtr, z, surface);
    }

    public void sendUibcEvent(String str) {
        nativeSendUibcEvent(this.mPtr, str);
    }

    private void notifyDisplayConnected(final Surface surface, final int i, final int i2, final int i3, final int i4) {
        Slog.d(TAG, "notifyDisplayConnected");
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                RemoteDisplay.this.mListener.onDisplayConnected(surface, i, i2, i3, i4);
            }
        });
    }

    private void notifyDisplayDisconnected() {
        Slog.d(TAG, "notifyDisplayDisconnected");
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                RemoteDisplay.this.mListener.onDisplayDisconnected();
            }
        });
    }

    private void notifyDisplayError(final int i) {
        Slog.d(TAG, "notifyDisplayError");
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                RemoteDisplay.this.mListener.onDisplayError(i);
            }
        });
    }

    private void notifyDisplayKeyEvent(final int i, final int i2) {
        Slog.d(TAG, "notifyDisplayKeyEvent");
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                RemoteDisplay.this.mListener.onDisplayKeyEvent(i, i2);
            }
        });
    }

    private void notifyDisplayGenericMsgEvent(final int i) {
        Slog.d(TAG, "notifyDisplayGenericMsgEvent");
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                RemoteDisplay.this.mListener.onDisplayGenericMsgEvent(i);
            }
        });
    }
}
