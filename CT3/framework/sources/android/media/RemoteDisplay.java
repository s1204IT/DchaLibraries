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

    private RemoteDisplay(Listener listener, Handler handler, String opPackageName) {
        this.mListener = listener;
        this.mHandler = handler;
        this.mOpPackageName = opPackageName;
    }

    protected void finalize() throws Throwable {
        try {
            dispose(true);
        } finally {
            super.finalize();
        }
    }

    public static RemoteDisplay listen(String iface, Listener listener, Handler handler, String opPackageName) {
        Slog.d(TAG, "listen");
        if (iface == null) {
            throw new IllegalArgumentException("iface must not be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        RemoteDisplay display = new RemoteDisplay(listener, handler, opPackageName);
        display.startListening(iface);
        return display;
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

    private void dispose(boolean finalized) {
        Slog.d(TAG, "dispose");
        if (this.mPtr != 0) {
            if (this.mGuard != null) {
                if (finalized) {
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

    private void startListening(String iface) {
        Slog.d(TAG, "startListening");
        this.mPtr = nativeListen(iface, this.mOpPackageName);
        if (this.mPtr == 0) {
            throw new IllegalStateException("Could not start listening for remote display connection on \"" + iface + "\"");
        }
        this.mGuard.open("dispose");
    }

    public void setBitrateControl(int bitrate) {
        nativeSetBitrateControl(this.mPtr, bitrate);
    }

    public int getWfdParam(int paramType) {
        return nativeGetWfdParam(this.mPtr, paramType);
    }

    public static RemoteDisplay connect(String iface, Surface surface, Listener listener, Handler handler) {
        Slog.d(TAG, "connect");
        if (iface == null) {
            throw new IllegalArgumentException("iface must not be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        RemoteDisplay display = new RemoteDisplay(listener, handler, null);
        display.startConnecting(iface, surface);
        return display;
    }

    private void startConnecting(String iface, Surface surface) {
        Slog.d(TAG, "startConnecting");
        this.mPtr = nativeConnect(iface, surface);
        if (this.mPtr == 0) {
            throw new IllegalStateException("Could not start connecting for remote display connection on \"" + iface + "\"");
        }
        this.mGuard.open("dispose");
    }

    public void suspendDisplay(boolean suspend, Surface surface) {
        Slog.d(TAG, "suspendDisplay");
        if (suspend && surface != null) {
            throw new IllegalArgumentException("surface must be null when suspend display");
        }
        if (!suspend && surface == null) {
            throw new IllegalArgumentException("surface must not be null when resume display");
        }
        nativeSuspendDisplay(this.mPtr, suspend, surface);
    }

    public void sendUibcEvent(String eventDesc) {
        nativeSendUibcEvent(this.mPtr, eventDesc);
    }

    private void notifyDisplayConnected(final Surface surface, final int width, final int height, final int flags, final int session) {
        Slog.d(TAG, "notifyDisplayConnected");
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                RemoteDisplay.this.mListener.onDisplayConnected(surface, width, height, flags, session);
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

    private void notifyDisplayError(final int error) {
        Slog.d(TAG, "notifyDisplayError");
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                RemoteDisplay.this.mListener.onDisplayError(error);
            }
        });
    }

    private void notifyDisplayKeyEvent(final int uniCode, final int flags) {
        Slog.d(TAG, "notifyDisplayKeyEvent");
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                RemoteDisplay.this.mListener.onDisplayKeyEvent(uniCode, flags);
            }
        });
    }

    private void notifyDisplayGenericMsgEvent(final int event) {
        Slog.d(TAG, "notifyDisplayGenericMsgEvent");
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                RemoteDisplay.this.mListener.onDisplayGenericMsgEvent(event);
            }
        });
    }
}
