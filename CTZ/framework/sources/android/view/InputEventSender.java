package android.view;

import android.os.Looper;
import android.os.MessageQueue;
import android.util.Log;
import dalvik.system.CloseGuard;
import java.lang.ref.WeakReference;

public abstract class InputEventSender {
    private static final String TAG = "InputEventSender";
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private InputChannel mInputChannel;
    private MessageQueue mMessageQueue;
    private long mSenderPtr;

    private static native void nativeDispose(long j);

    private static native long nativeInit(WeakReference<InputEventSender> weakReference, InputChannel inputChannel, MessageQueue messageQueue);

    private static native boolean nativeSendKeyEvent(long j, int i, KeyEvent keyEvent);

    private static native boolean nativeSendMotionEvent(long j, int i, MotionEvent motionEvent);

    public InputEventSender(InputChannel inputChannel, Looper looper) {
        if (inputChannel == null) {
            throw new IllegalArgumentException("inputChannel must not be null");
        }
        if (looper == null) {
            throw new IllegalArgumentException("looper must not be null");
        }
        this.mInputChannel = inputChannel;
        this.mMessageQueue = looper.getQueue();
        this.mSenderPtr = nativeInit(new WeakReference(this), inputChannel, this.mMessageQueue);
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
        if (this.mSenderPtr != 0) {
            nativeDispose(this.mSenderPtr);
            this.mSenderPtr = 0L;
        }
        this.mInputChannel = null;
        this.mMessageQueue = null;
    }

    public void onInputEventFinished(int i, boolean z) {
    }

    public final boolean sendInputEvent(int i, InputEvent inputEvent) {
        if (inputEvent == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (this.mSenderPtr == 0) {
            Log.w(TAG, "Attempted to send an input event but the input event sender has already been disposed.");
            return false;
        }
        if (inputEvent instanceof KeyEvent) {
            return nativeSendKeyEvent(this.mSenderPtr, i, (KeyEvent) inputEvent);
        }
        return nativeSendMotionEvent(this.mSenderPtr, i, (MotionEvent) inputEvent);
    }

    private void dispatchInputEventFinished(int i, boolean z) {
        onInputEventFinished(i, z);
    }
}
