package android.media;

import android.os.Handler;
import android.powerhint.PowerHintManager;
import android.view.Surface;
import dalvik.system.CloseGuard;

public final class RemoteDisplay {
    public static final int DISPLAY_ERROR_CONNECTION_DROPPED = 2;
    public static final int DISPLAY_ERROR_UNKOWN = 1;
    public static final int DISPLAY_FLAG_SECURE = 1;
    private final CloseGuard mGuard = CloseGuard.get();
    private final Handler mHandler;
    private final Listener mListener;
    private long mPtr;

    public interface Listener {
        void onDisplayConnected(Surface surface, int i, int i2, int i3, int i4);

        void onDisplayDisconnected();

        void onDisplayError(int i);
    }

    private native void nativeDispose(long j);

    private native long nativeListen(String str);

    private native void nativePause(long j);

    private native void nativeResume(long j);

    private RemoteDisplay(Listener listener, Handler handler) {
        this.mListener = listener;
        this.mHandler = handler;
    }

    protected void finalize() throws Throwable {
        try {
            dispose(true);
        } finally {
            super.finalize();
        }
    }

    public static RemoteDisplay listen(String iface, Listener listener, Handler handler) {
        if (iface == null) {
            throw new IllegalArgumentException("iface must not be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        RemoteDisplay display = new RemoteDisplay(listener, handler);
        display.startListening(iface);
        return display;
    }

    public void dispose() {
        dispose(false);
    }

    public void pause() {
        nativePause(this.mPtr);
    }

    public void resume() {
        nativeResume(this.mPtr);
    }

    private void dispose(boolean finalized) {
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
            PowerHintManager phMgr = new PowerHintManager();
            phMgr.sendPowerHint(PowerHintManager.HINT_WFD, "disable");
        }
    }

    private void startListening(String iface) {
        PowerHintManager phMgr = new PowerHintManager();
        phMgr.sendPowerHint(PowerHintManager.HINT_WFD, "enable");
        this.mPtr = nativeListen(iface);
        if (this.mPtr == 0) {
            phMgr.sendPowerHint(PowerHintManager.HINT_WFD, "disable");
            throw new IllegalStateException("Could not start listening for remote display connection on \"" + iface + "\"");
        }
        this.mGuard.open("dispose");
    }

    private void notifyDisplayConnected(final Surface surface, final int width, final int height, final int flags, final int session) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                RemoteDisplay.this.mListener.onDisplayConnected(surface, width, height, flags, session);
            }
        });
    }

    private void notifyDisplayDisconnected() {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                RemoteDisplay.this.mListener.onDisplayDisconnected();
            }
        });
    }

    private void notifyDisplayError(final int error) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                RemoteDisplay.this.mListener.onDisplayError(error);
            }
        });
    }
}
