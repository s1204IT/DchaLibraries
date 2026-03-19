package android.view;

import android.os.Looper;
import android.os.MessageQueue;
import android.view.Window;
import com.android.internal.util.VirtualRefBasePtr;
import java.lang.ref.WeakReference;

public class FrameMetricsObserver {
    private FrameMetrics mFrameMetrics;
    Window.OnFrameMetricsAvailableListener mListener;
    private MessageQueue mMessageQueue;
    VirtualRefBasePtr mNative;
    private WeakReference<Window> mWindow;

    FrameMetricsObserver(Window window, Looper looper, Window.OnFrameMetricsAvailableListener onFrameMetricsAvailableListener) {
        if (looper == null) {
            throw new NullPointerException("looper cannot be null");
        }
        this.mMessageQueue = looper.getQueue();
        if (this.mMessageQueue == null) {
            throw new IllegalStateException("invalid looper, null message queue\n");
        }
        this.mFrameMetrics = new FrameMetrics();
        this.mWindow = new WeakReference<>(window);
        this.mListener = onFrameMetricsAvailableListener;
    }

    private void notifyDataAvailable(int i) {
        Window window = this.mWindow.get();
        if (window != null) {
            this.mListener.onFrameMetricsAvailable(window, this.mFrameMetrics, i);
        }
    }
}
