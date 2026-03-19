package android.view;

import android.os.Looper;
import android.os.MessageQueue;
import android.util.Log;
import dalvik.annotation.optimization.FastNative;
import dalvik.system.CloseGuard;
import java.lang.ref.WeakReference;

public abstract class DisplayEventReceiver {
    private static final String TAG = "DisplayEventReceiver";
    public static final int VSYNC_SOURCE_APP = 0;
    public static final int VSYNC_SOURCE_SURFACE_FLINGER = 1;
    private final CloseGuard mCloseGuard;
    private MessageQueue mMessageQueue;
    private long mReceiverPtr;

    private static native void nativeDispose(long j);

    private static native long nativeInit(WeakReference<DisplayEventReceiver> weakReference, MessageQueue messageQueue, int i);

    @FastNative
    private static native void nativeScheduleVsync(long j);

    public DisplayEventReceiver(Looper looper) {
        this(looper, 0);
    }

    public DisplayEventReceiver(Looper looper, int i) {
        this.mCloseGuard = CloseGuard.get();
        if (looper == null) {
            throw new IllegalArgumentException("looper must not be null");
        }
        this.mMessageQueue = looper.getQueue();
        this.mReceiverPtr = nativeInit(new WeakReference(this), this.mMessageQueue, i);
        this.mCloseGuard.open("dispose");
    }

    protected void finalize() throws Throwable {
        try {
            dispose(true);
        } finally {
            super.finalize();
        }
    }

    public void dispose() {
        dispose(false);
    }

    private void dispose(boolean z) {
        if (this.mCloseGuard != null) {
            if (z) {
                this.mCloseGuard.warnIfOpen();
            }
            this.mCloseGuard.close();
        }
        if (this.mReceiverPtr != 0) {
            nativeDispose(this.mReceiverPtr);
            this.mReceiverPtr = 0L;
        }
        this.mMessageQueue = null;
    }

    public void onVsync(long j, int i, int i2) {
    }

    public void onHotplug(long j, int i, boolean z) {
    }

    public void scheduleVsync() {
        if (this.mReceiverPtr == 0) {
            Log.w(TAG, "Attempted to schedule a vertical sync pulse but the display event receiver has already been disposed.");
        } else {
            nativeScheduleVsync(this.mReceiverPtr);
        }
    }

    private void dispatchVsync(long j, int i, int i2) {
        onVsync(j, i, i2);
    }

    private void dispatchHotplug(long j, int i, boolean z) {
        onHotplug(j, i, z);
    }
}
